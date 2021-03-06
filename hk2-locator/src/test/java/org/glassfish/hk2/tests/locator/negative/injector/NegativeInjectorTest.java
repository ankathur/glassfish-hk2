/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.hk2.tests.locator.negative.injector;

import java.lang.reflect.Field;
import java.util.List;

import junit.framework.Assert;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.UnsatisfiedDependencyException;
import org.glassfish.hk2.tests.locator.utilities.LocatorHelper;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.junit.Test;

/**
 * A set of tests for negative path
 * 
 * @author jwells
 */
public class NegativeInjectorTest {
    private final static String TEST_NAME = "NegativeInjectorTest";
    private final static ServiceLocator locator = LocatorHelper.create(TEST_NAME, new NegativeInjectorModule());
    
    /**
     * No class, I tell ya, no class!
     */
    public final static String NO_CLASS = "this.class.is.not.Here";
    
    /**
     * null to create
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullCreate() {
        locator.create(null);
    }
    
    /**
     * null to inject
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullInject() {
        locator.inject(null);
    }
    
    /**
     * null to post construct
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullPostConstruct() {
        locator.postConstruct(null);
    }
    
    /**
     * null to preDestroy
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullPreDestroy() {
        locator.preDestroy(null);
    }
    
    /**
     * null to preDestroy
     */
    @Test
    public void testConstructorThrows() {
        try {
            locator.create(ThrowyC.class);
            Assert.fail("ThrowyC throws in its constructor");
        }
        catch (MultiException me) {
            Assert.assertTrue("Expected " + LocatorHelper.EXPECTED + " but got " + me.getMessage(), me.getMessage().contains(LocatorHelper.EXPECTED));
        }
    }
    
    private final static String FIELD_EXPECTED = " may not be static, final or have an Annotation type";
    
    /**
     * tries to inject into a bad string
     */
    @Test
    public void testBadField() {
        ThrowyF tf = new ThrowyF();
        
        try {
            locator.inject(tf);
            Assert.fail("ThrowyF has a final field to be injected");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(), me.getMessage().contains(FIELD_EXPECTED));
        }
    }
    
    /**
     * post construct throws
     */
    @Test
    public void testBadPostConstruct() {
        ThrowyPC tpc = new ThrowyPC();
        
        try {
            locator.postConstruct(tpc);
            Assert.fail("ThrowyPC throws in its post construct");
        }
        catch (MultiException me) {
            Assert.assertTrue("Expected " + LocatorHelper.EXPECTED + " but got " + me.getMessage(),
                    me.getMessage().contains(LocatorHelper.EXPECTED));
        }
    }
    
    /**
     * pre destroy throws
     */
    @Test
    public void testBadPreDestroy() {
        ThrowyPC tpc = new ThrowyPC();
        
        try {
            locator.preDestroy(tpc);
            Assert.fail("ThrowyPC throws in its pre destroy");
        }
        catch (MultiException me) {
            Assert.assertTrue("Expected " + LocatorHelper.EXPECTED + " but got " + me.getMessage(),
                    me.getMessage().contains(LocatorHelper.EXPECTED));
        }
    }
    
    /**
     * method body throws
     */
    @Test
    public void testBadMethod() {
        ThrowyM tpc = new ThrowyM();
        
        try {
            locator.inject(tpc);
            Assert.fail("ThrowyM throws in its initializer method");
        }
        catch (MultiException me) {
            Assert.assertTrue("Expected " + LocatorHelper.EXPECTED + " but got " + me.getMessage(),
                    me.getMessage().contains(LocatorHelper.EXPECTED));
        }
    }
    
    /**
     * tests a classloader failure (since there really is no class)
     */
    @Test
    public void testNoClass() {
        ActiveDescriptor<?> ad = locator.getBestDescriptor(BuilderHelper.createContractFilter(NO_CLASS));
        Assert.assertNotNull(ad);
        
        ServiceHandle<?> handle = locator.getServiceHandle(ad);
        Assert.assertNotNull(handle);
        
        try {
            handle.getService();
        }
        catch (MultiException me) {
            // Two exceptions, one from the HK2 classloader, one from the CCL
            List<Throwable> thList = me.getErrors();
            Assert.assertEquals(2, thList.size());
            
            Throwable th = thList.get(0);
            
            Assert.assertTrue(th instanceof ClassNotFoundException);
            
            Throwable th2 = thList.get(1);
            
            Assert.assertTrue(th2 instanceof ClassNotFoundException);
        }
    }
    
    /**
     * Tests an injection resolver that does not have a parameterized type
     */
    @Test
    public void testRawInjectionResolver() {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.bind(BuilderHelper.link(RawInjectionResolver.class).
                to(InjectionResolver.class).build());
        
        try {
            config.commit();
            Assert.fail("Bad injection resolver should have caused commit to fail");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(),
                    me.getMessage().contains("An implementation of InjectionResolver must be a parameterized type and the actual type" +
                            " must be an annotation"));
        }
    }
    
    /**
     * Tests an injection resolver that has a type variable for its type
     */
    @Test
    public void testTypeVariableInjectionResolver() {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.bind(BuilderHelper.link(TypeVariableInjectionResolver.class).
                to(InjectionResolver.class).build());
        
        try {
            config.commit();
            Assert.fail("Bad injection resolver should have caused commit to fail");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(),
                    me.getMessage().contains("An implementation of InjectionResolver must be a parameterized type and the actual type" +
                            " must be an annotation"));
        }
    }
    
    /**
     * Tests an injection resolver that has a non annotation as its actual type
     */
    @Test
    public void testNotAnnotationInjectionResolver() {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.bind(BuilderHelper.link(NotAnnotationInjectionResolver.class).
                to(InjectionResolver.class).build());
        
        try {
            config.commit();
            Assert.fail("Bad injection resolver should have caused commit to fail");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(),
                    me.getMessage().contains("An implementation of InjectionResolver must be a parameterized type and the actual type" +
                            " must be an annotation"));
        }
    }
    
    /**
     * Tests an attempt to get a descriptor for a bad injectee
     */
    @Test
    public void testInvalidInjectee() {
        try {
            locator.getInjecteeDescriptor(new InjecteeImpl());
            Assert.fail("Bad injectee should have caused this to fail");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(),
                    me.getMessage().contains("Invalid injectee with required type of "));
        }
    }
    
    /**
     * This post construct has a bad parameter
     */
    @Test
    public void testInvalidPostConstruct() {
        BadPC badPC = new BadPC();
        
        try {
            locator.postConstruct(badPC);
            Assert.fail("This post construct should have caused failure");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(), me.getMessage().contains(
                    " annotated with @PostConstruct must not have any arguments"));
        }
    }
    
    /**
     * This post construct has a bad parameter
     */
    @Test
    public void testInvalidPreDestroy() {
        BadPD badPD = new BadPD();
        
        try {
            locator.preDestroy(badPD);
            Assert.fail("This pre destroy should have caused failure");
        }
        catch (MultiException me) {
            Assert.assertTrue(me.getMessage(), me.getMessage().contains(
                    " annotated with @PreDestroy must not have any arguments"));
        }
    }
    
    /**
     * Tests that a service that tries to inject an unknown service will get
     * the proper failure
     */
    @Test
    public void testInvalidInjectionPoint() {
        InjectsAClassThatIsNotAService iactinas = locator.create(InjectsAClassThatIsNotAService.class);
        Assert.assertNotNull(iactinas);
        
        Injectee badInjectee = null;
        try {
            locator.inject(iactinas);
            Assert.fail("This injection should have failed");
        }
        catch (MultiException me) {
            for (Throwable th : me.getErrors()) {
                if (th instanceof UnsatisfiedDependencyException) {
                    Assert.assertNull(badInjectee);  // Should only be one of these
                    
                    UnsatisfiedDependencyException ude = (UnsatisfiedDependencyException) th;
                    
                    badInjectee = ude.getInjectee();
                }
            }
        }
        
        Assert.assertNotNull(badInjectee);
        
        // OK, lets do a check of its fields
        Assert.assertEquals(NotAService.class, badInjectee.getRequiredType());
        Assert.assertTrue(badInjectee.getRequiredQualifiers().isEmpty());
        Assert.assertSame(-1, badInjectee.getPosition());
        Assert.assertEquals(InjectsAClassThatIsNotAService.class, badInjectee.getInjecteeClass());
        Assert.assertTrue(badInjectee.getParent() instanceof Field);
        Assert.assertFalse(badInjectee.isOptional());
        Assert.assertFalse(badInjectee.isSelf());
        Assert.assertNull(badInjectee.getUnqualified());
    }

}
