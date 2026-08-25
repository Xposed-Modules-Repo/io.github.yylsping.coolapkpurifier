package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.List;

final class EntityListFilter {
    private final EntityClassifier classifier;
    private final EntityClassifier.Context context;

    EntityListFilter(EntityClassifier classifier) {
        this(classifier, EntityClassifier.Context.FEED);
    }

    EntityListFilter(EntityClassifier classifier, EntityClassifier.Context context) {
        this.classifier = classifier;
        this.context = context;
    }

    List<?> filter(List<?> source) {
        int firstAd = -1;
        for (int index = 0; index < source.size(); index++) {
            if (classifier.shouldRemove(source.get(index), context)) {
                firstAd = index;
                break;
            }
        }
        if (firstAd < 0) {
            return source;
        }

        ArrayList<Object> clean = new ArrayList<>(source.size() - 1);
        for (int index = 0; index < firstAd; index++) {
            clean.add(source.get(index));
        }
        for (int index = firstAd + 1; index < source.size(); index++) {
            Object item = source.get(index);
            if (!classifier.shouldRemove(item, context)) {
                clean.add(item);
            }
        }
        return clean;
    }
}
