package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.util.Utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AngularJSClientExtension extends EmitterExtension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.generatesModuleCode = true;
        features.generatesJaxrsApplicationClient = true;
        features.restResponseType = "ng.IPromise<R>";
        features.restOptionsType = "<O>";
        features.npmPackageDependencies = Collections.singletonMap("angular", "=1.6.4");
        return features;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        emitSharedPart(writer, settings);
        for (TsBeanModel bean : model.getBeans()) {
            if (bean.isJaxrsApplicationClientBean()) {
                final String clientName = bean.getName().toString();
                emitClient(writer, settings, exportKeyword, clientName);
            }
        }
    }

    private void emitSharedPart(Writer writer, Settings settings) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AngularJSClientExtension-shared.template.ts"));
        Emitter.writeTemplate(writer, settings, template, null);
    }

    private void emitClient(Writer writer, Settings settings, boolean exportKeyword, String clientName) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AngularJSClientExtension-client.template.ts"));
        final Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("\"", settings.quotes);
        replacements.put("/*export*/ ", exportKeyword ? "export " : "");
        replacements.put("$$RestApplicationClient$$", clientName);
        replacements.put("$$AngularJSRestApplicationClient$$", "AngularJS" + clientName);
        Emitter.writeTemplate(writer, settings, template, replacements);
    }

}
