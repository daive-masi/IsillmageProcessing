package ImageProcessing.Core;

/**
 * Classe utilitaire pour ajouter du padding (bordure) aux images (matrices int[][]).
 */
public class PaddingUtils {

    /**
     * Ajoute un padding à une image (matrice int[y][x]).
     *
     * @param image L'image originale (int[hauteur][largeur]).
     * @param padSize La taille du padding à ajouter de chaque côté (en pixels).
     *                Pour un masque de taille NxN (N impair), padSize = (N-1)/2.
     * @param mode La stratégie de remplissage des bords (BorderMode.ZERO, REPLICATE, MIRROR).
     * @return Une nouvelle image (int[hauteur + 2*padSize][largeur + 2*padSize]) avec le padding,
     *         ou null si l'entrée est invalide.
     */
    public static int[][] padImage(int[][] image, int padSize, BorderMode mode) {
        if (image == null || image.length == 0 || image[0].length == 0 || padSize < 0) {
            System.err.println("PaddingUtils.padImage: Invalid input.");
            return null;
        }
        if (padSize == 0) {
            // Pas de padding nécessaire, mais retournons une copie pour la cohérence
            return cloneMatrix(image);
        }

        int originalHeight = image.length;
        int originalWidth = image[0].length;
        int newHeight = originalHeight + 2 * padSize;
        int newWidth = originalWidth + 2 * padSize;

        int[][] paddedImage = new int[newHeight][newWidth];

        // 1. Copier l'image originale au centre de la nouvelle image
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                paddedImage[y + padSize][x + padSize] = image[y][x];
            }
        }

        // 2. Remplir les zones de padding (bords et coins) selon le mode
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Si on est dans la zone originale copiée, on ne fait rien
                if (y >= padSize && y < originalHeight + padSize && x >= padSize && x < originalWidth + padSize) {
                    continue;
                }

                // Sinon, on est dans le padding, calculer la valeur
                int sourceY = y - padSize;
                int sourceX = x - padSize;

                switch (mode) {
                    case ZERO:
                        paddedImage[y][x] = 0;
                        break;

                    case REPLICATE:
                        // Ramener les indices source dans les limites de l'image originale
                        int clampedY = Math.max(0, Math.min(originalHeight - 1, sourceY));
                        int clampedX = Math.max(0, Math.min(originalWidth - 1, sourceX));
                        paddedImage[y][x] = image[clampedY][clampedX];
                        break;

                    case MIRROR:
                        // Gérer la réflexion en miroir
                        // Exemple simple pour y (à adapter pour x et les coins)
                        int mirroredY = sourceY;
                        if (mirroredY < 0) {
                            mirroredY = -mirroredY -1; // Miroir par rapport à la ligne 0
                        } else if (mirroredY >= originalHeight) {
                            mirroredY = (originalHeight - 1) - (mirroredY - originalHeight) ; // Miroir par rapport à la dernière ligne
                        }
                        // Gérer la réflexion en miroir pour x
                        int mirroredX = sourceX;
                        if (mirroredX < 0) {
                            mirroredX = -mirroredX -1; // Miroir par rapport à la colonne 0
                        } else if (mirroredX >= originalWidth) {
                            mirroredX = (originalWidth - 1) - (mirroredX - originalWidth) ; // Miroir par rapport à la dernière colonne
                        }

                        // S'assurer que les indices miroités restent valides (double miroir possible aux coins)
                        mirroredY = Math.max(0, Math.min(originalHeight - 1, mirroredY));
                        mirroredX = Math.max(0, Math.min(originalWidth - 1, mirroredX));


                        paddedImage[y][x] = image[mirroredY][mirroredX];
                        break;

                    default: // Cas par défaut (ou si mode est null), on met 0
                        paddedImage[y][x] = 0;
                        break;
                }
            }
        }

        return paddedImage;
    }

    /**
     * Fonction utilitaire pour cloner une matrice int[][].
     * @param matrix Matrice à cloner.
     * @return Une copie profonde de la matrice.
     */
    public static int[][] cloneMatrix(int[][] matrix) {
        if (matrix == null) return null;
        int height = matrix.length;
        if (height == 0) return new int[0][];
        int width = matrix[0].length;
        int[][] copy = new int[height][width];
        for (int y = 0; y < height; y++) {
            // Utiliser System.arraycopy pour la performance sur les lignes
            System.arraycopy(matrix[y], 0, copy[y], 0, width);
            // Alternative ligne par ligne:
            // for (int x = 0; x < width; x++) {
            //    copy[y][x] = matrix[y][x];
            // }
        }
        return copy;
    }
}