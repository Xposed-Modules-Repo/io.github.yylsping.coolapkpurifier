package io.github.yylsping.coolapkpurifier;

import org.json.JSONObject;

import java.util.List;

/**
 * Exact card-ViewHolder binder contract for the search hot-word / hot-ranking
 * UI terminals. One spec shape serves all three terminals (home-ranking
 * hot-word capsule, search-page hot-search card, search-page hot-list card);
 * the manifest target key ({@code kind}) distinguishes them.
 *
 * <p>Classification is render-time only: the bound {@code Card} must carry
 * the pinned entity template, and when {@code urlContains} is non-empty the
 * card url must contain it (the capsule template is shared with non-hot-word
 * content on mixed pages).</p>
 */
final class SearchUiCardTargetSpec {
    final String kind;
    final String ownerClass;
    final String methodName;
    final String dataClass;
    final String cardClass;
    final String entityTemplateGetter;
    final String entityTemplate;
    final String urlGetter;
    final String urlContains;
    final List<String> constructorParams;
    final String viewHolderClass;

    private SearchUiCardTargetSpec(String kind, String ownerClass, String methodName,
                                   String dataClass, String cardClass,
                                   String entityTemplateGetter, String entityTemplate,
                                   String urlGetter, String urlContains,
                                   List<String> constructorParams,
                                   String viewHolderClass) {
        this.kind = kind;
        this.ownerClass = ownerClass;
        this.methodName = methodName;
        this.dataClass = dataClass;
        this.cardClass = cardClass;
        this.entityTemplateGetter = entityTemplateGetter;
        this.entityTemplate = entityTemplate;
        this.urlGetter = urlGetter;
        this.urlContains = urlContains;
        this.constructorParams = constructorParams;
        this.viewHolderClass = viewHolderClass;
    }

    String descriptor() {
        return DescriptorUtils.classDescriptorOf(ownerClass) + "->" + methodName
                + "(" + DescriptorUtils.classDescriptorOf(dataClass) + ")V";
    }

    static SearchUiCardTargetSpec parse(JSONObject json, String kind)
            throws TargetManifest.ManifestException {
        if (json == null) {
            return null;
        }
        return new SearchUiCardTargetSpec(kind,
                SpecJson.required(json, "ownerClass", kind),
                SpecJson.required(json, "methodName", kind),
                SpecJson.required(json, "dataClass", kind),
                SpecJson.required(json, "cardClass", kind),
                SpecJson.required(json, "entityTemplateGetter", kind),
                SpecJson.required(json, "entityTemplate", kind),
                SpecJson.required(json, "urlGetter", kind),
                json.optString("urlContains", ""),
                SpecJson.requiredArray(json, "constructorParams", kind),
                SpecJson.required(json, "viewHolderClass", kind));
    }
}
