package com.redbeemedia.enigma.referenceapp.image;

import com.redbeemedia.enigma.exposureutils.models.image.ApiImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageUtil {
    public static ApiImage getBestFit(List<ApiImage> images, String orientation, int targetWidth, int targetHeight) {
        if(images.isEmpty()) {
            return null;
        } else {
            List<ApiImage> filteredImages = new ArrayList<>();
            for(ApiImage image : images) {
                if(orientation.equals(image.getOrientation()) && image.getUrl() != null) {
                    filteredImages.add(image);
                }
            }
            if(filteredImages.isEmpty()) {
                return null;
            }
            return Collections.max(filteredImages, new ImageFitComparator(new ImageFitScoreCalculator("thumbnail" ,targetWidth, targetHeight)));
        }
    }

    private static class ImageFitScoreCalculator implements IImageFitScoreCalculator {
        private final String requestedType;
        private final int targetWidth;
        private final int targetHeight;

        public ImageFitScoreCalculator(String requestedType, int targetWidth, int targetHeight) {
            this.requestedType = requestedType;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        @Override
        public float getScore(ApiImage image) {
            float score = 0f;

            if(requestedType.equals(image.getType())) {
                score += 1000000f;
            }
            score += getDimensionScore((int) image.getWidth(), targetWidth);
            score += getDimensionScore((int) image.getHeight(), targetHeight);

            return score;
        }

        private static float getDimensionScore(int dim, int targetDim) {
            float diff = dim-targetDim;
            if(diff < 0) {
                return 0f;
            } else {
                return 1000000f/(1f+(diff/50f)); //diff == 0 -> full score, diff == 50 --> half score, etc
            }
        }
    }

    private interface IImageFitScoreCalculator {
        float getScore(ApiImage image);
    }

    private static class ImageFitComparator implements Comparator<ApiImage> {
        private final IImageFitScoreCalculator scoreCalculator;

        public ImageFitComparator(IImageFitScoreCalculator scoreCalculator) {
            this.scoreCalculator = scoreCalculator;
        }

        @Override
        public int compare(ApiImage o1, ApiImage o2) {
            return Float.compare(scoreCalculator.getScore(o1), scoreCalculator.getScore(o2));
        }
    }
}
