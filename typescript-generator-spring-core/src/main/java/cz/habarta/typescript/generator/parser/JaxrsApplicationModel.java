
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class JaxrsApplicationModel {

    private String applicationPath;
    private String applicationName;
    private final List<JaxrsMethodModel> methods = new ArrayList<>();

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public List<JaxrsMethodModel> getMethods() {
        return methods;
    }

    public static JaxrsApplicationModel concatModel(JaxrsApplicationModel a, JaxrsApplicationModel b) {
        JaxrsApplicationModel result = new JaxrsApplicationModel();

        result.applicationPath = a.applicationPath != null ? a.applicationPath : b.applicationPath;
        result.applicationName = a.applicationName != null ? a.applicationName : b.applicationName;
        result.methods.addAll(a.getMethods());
        result.methods.addAll(b.getMethods());

        return result;
    }

}
