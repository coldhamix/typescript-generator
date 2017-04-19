package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Parameter;
import cz.habarta.typescript.generator.util.Predicate;
import cz.habarta.typescript.generator.util.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.ws.rs.Path;
import javax.ws.rs.core.GenericEntity;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


public class SpringApplicationParser {

    private final Settings settings;
    private final Predicate<String> isClassNameExcluded;
    private final JaxrsApplicationModel model;

    public SpringApplicationParser(Settings settings) {
        this.settings = settings;
        this.isClassNameExcluded = settings.getExcludeFilter();
        this.model = new JaxrsApplicationModel();
    }

    private static RequestMethod getRequestMethod(Method method) {
        RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
        if (requestMappingAnnotation != null) {
            RequestMethod[] methods = requestMappingAnnotation.method();
            return methods.length > 0 ? methods[0] : RequestMethod.POST;
        }
        return null;
    }

    private static String getRequestMethodName(RequestMethod requestMethod) {
        if (requestMethod != null) {
            return requestMethod.name();
        }
        return null;
    }

    private static MethodParameterModel getEntityParameter(Method method) {
        final List<Parameter> parameters = Parameter.ofMethod(method);
        for (Parameter parameter : parameters) {
            RequestBody requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
            if (requestBodyAnnotation != null) {
                return new MethodParameterModel(parameter.getName(), parameter.getParameterizedType());
            }
        }
        return null;
    }

    public static List<Class<?>> getStandardEntityClasses() {
        // JAX-RS specification - 4.2.4 Standard Entity Providers
        return Arrays.asList(
                byte[].class,
                String.class,
                java.io.InputStream.class,
                java.io.Reader.class,
                java.io.File.class,
                Boolean.class, Character.class, Number.class,
                long.class, int.class, short.class, byte.class, double.class, float.class, boolean.class, char.class);
    }

    public JaxrsApplicationModel getModel() {
        return model;
    }

    public Result tryParse(SourceType<?> sourceType) {
        if (!(sourceType.type instanceof Class<?>)) {
            return null;
        }
        final Class<?> cls = (Class<?>) sourceType.type;

        // resource
        final Controller controller = cls.getAnnotation(Controller.class);
        final RequestMapping requestMapping = cls.getAnnotation(RequestMapping.class);
        if (controller != null) {
            final Result result = new Result();

            String controllerPath = requestMapping != null ? requestMapping.value()[0] : null;
            System.out.println("Parsing Spring controller: " + cls.getName() + " available by path " + controllerPath);

            parseResource(result, new ResourceContext(cls, controllerPath), cls);
            return result;
        }

        return null;
    }

    private void parseResource(Result result, ResourceContext context, Class<?> controllerClass) {
        final List<Method> methods = Arrays.asList(controllerClass.getMethods());
        Collections.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Method method : methods) {
            parseResourceMethod(result, context, controllerClass, method);
        }
    }

    private void parseResourceMethod(Result result, ResourceContext context, Class<?> resourceClass, Method method) {

        RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
        if (requestMappingAnnotation != null) {
            final String path = requestMappingAnnotation.value()[0];

            Type responseType = parseResponseType(result, resourceClass, method);
            MethodParameterModel request = parseRequestType(result, resourceClass, method);
            RequestMethod requestMethod = getRequestMethod(method);

            if (requestMethod == null) {
                requestMethod = RequestMethod.POST;
            }

            // create method
            model.getMethods().add(
                    new JaxrsMethodModel(
                            resourceClass,
                            methodNameFromUrl(context.path + path),
                            responseType,
                            context.rootResource,
                            getRequestMethodName(requestMethod),
                            context.path + path,
                            Collections.<MethodParameterModel>emptyList(),
                            Collections.<MethodParameterModel>emptyList(),
                            request,
                            null
                    ));
        }
    }

    private Type parseResponseType(Result result, Class<?> resourceClass, Method method) {
        ResponseBody responseBodyAnnotation = method.getAnnotation(ResponseBody.class);
        if (responseBodyAnnotation != null) {
            final Class<?> returnType = method.getReturnType();
            final Type genericReturnType = method.getGenericReturnType();
            final Type modelReturnType;
            if (returnType == void.class) {
                modelReturnType = returnType;
            } else if (genericReturnType instanceof ParameterizedType && returnType == GenericEntity.class) {
                final ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
                modelReturnType = parameterizedReturnType.getActualTypeArguments()[0];
                foundType(result, modelReturnType, resourceClass, method.getName());
            } else {
                modelReturnType = genericReturnType;
                foundType(result, modelReturnType, resourceClass, method.getName());
            }
            return modelReturnType;
        }
        return void.class;
    }

    private MethodParameterModel parseRequestType(Result result, Class<?> resourceClass, Method method) {
        final MethodParameterModel entityParameter = getEntityParameter(method);
        if (entityParameter != null) {
            foundType(result, entityParameter.getType(), resourceClass, method.getName());
            return entityParameter;
        }
        return null;
    }

    private void foundType(Result result, Type type, Class<?> usedInClass, String usedInMember) {
        if (!isExcluded(type)) {
            result.discoveredTypes.add(new SourceType<>(type, usedInClass, usedInMember));
        }
    }

    private boolean isExcluded(Type type) {
        final Class<?> cls = Utils.getRawClassOrNull(type);
        if (cls == null) {
            return false;
        }
        if (isClassNameExcluded != null && isClassNameExcluded.test(cls.getName())) {
            return true;
        }

        for (Class<?> standardEntityClass : getStandardEntityClasses()) {
            if (standardEntityClass.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    private String methodNameFromUrl(String url) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] splitParts = splitUrl(url);

        for (int i = 0; i < splitParts.length; i++) {
            stringBuilder.append(i == 0 ?
                    splitParts[i].substring(0, 1).toLowerCase() :
                    splitParts[i].substring(0, 1).toUpperCase()
            );
            stringBuilder.append(splitParts[i].substring(1));
        }
        return stringBuilder.toString();
    }

    private String[] splitUrl(String url) {
        List<String> splitParts = new ArrayList<>();
        for (String part : url.split("/")) {
            if (!StringUtils.isEmpty(part.trim())) {
                splitParts.add(part);
            }
        }
        return splitParts.toArray(new String[]{});
    }

    public static class Result {
        public List<SourceType<Type>> discoveredTypes;

        public Result() {
            discoveredTypes = new ArrayList<>();
        }

        public Result(List<SourceType<Type>> discoveredTypes) {
            this.discoveredTypes = discoveredTypes;
        }
    }

    private static class ResourceContext {
        public final Class<?> rootResource;
        public final String path;
        public final Map<String, Type> pathParamTypes;

        public ResourceContext(Class<?> rootResource, String path) {
            this(rootResource, path, new LinkedHashMap<String, Type>());
        }

        private ResourceContext(Class<?> rootResource, String path, Map<String, Type> pathParamTypes) {
            this.rootResource = rootResource;
            this.path = path;
            this.pathParamTypes = pathParamTypes;
        }

        ResourceContext subPath(Path pathAnnotation) {
            final String subPath = pathAnnotation != null ? pathAnnotation.value() : null;
            return new ResourceContext(rootResource, Utils.joinPath(path, subPath), pathParamTypes);
        }

        ResourceContext subPathParamTypes(Map<String, Type> subPathParamTypes) {
            final Map<String, Type> newPathParamTypes = new LinkedHashMap<>();
            newPathParamTypes.putAll(pathParamTypes);
            if (subPathParamTypes != null) {
                newPathParamTypes.putAll(subPathParamTypes);
            }
            return new ResourceContext(rootResource, path, newPathParamTypes);
        }
    }

}
