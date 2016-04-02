/*
 * Copyright 2016 tamas.csaba@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsplode.synapse.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Csaba Tamas
 */
public class Reflector {

    public static List<Class> extractInterfaces(Class clazz) {
        List<Class> interfaces = new ArrayList<>();
        interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
        HashSet<Class<?>> superClasses = new HashSet<>();
        extractClassInheritance(clazz, superClasses);
        if (!superClasses.isEmpty()) {
            superClasses.stream().forEach((superClass) -> {
                interfaces.addAll(Arrays.asList(superClass.getInterfaces()));
            });

        }
        return interfaces;
    }

//    public static List<Method> getMethods(Class clazz){
//        List<Method> methods = new ArrayList<>();
//        methods.addAll(Arrays.asList(clazz.getMethods()));
//    }
    
    public static List<Class> filterInterfaces(List<Class> source, Class predicate) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        return source.stream().filter(i -> predicate.isAssignableFrom(i)).collect(Collectors.toList());
    }

    public static void extractClassInheritance(Class<?> clazz, Set<Class<?>> resultSet) {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            resultSet.add(superClass);
            extractClassInheritance(superClass, resultSet);
        }
    }
}
