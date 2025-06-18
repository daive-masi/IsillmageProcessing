package ImageProcessing.Core;

import CImage.CImage;
import CImage.CImageNG;
import CImage.Exceptions.CImageNGException;
import java.awt.image.BufferedImage;

/**
 * Classe utilitaire pour les conversions d'images et autres opérations de base.
 * 
 * CONVENTION IMPORTANTE : Dans toute la librairie ImageProcessing.*,
 * les matrices d'image (int[][]) utilisent la convention [y][x] (ligne, colonne),
 * où y représente l'indice de la ligne (hauteur) et x l'indice de la colonne (largeur).
 * Les classes CImageNG utilisent potentiellement une convention [x][y] en interne
 * (via getMatrice/setMatrice). Cette classe ImageUtils fait le pont entre les deux.
 */
public class ImageUtils {
    /**
     * Fonction utilitaire pour cloner une matrice int[][].
     * Crée une copie profonde de la matrice.
     *
     * @param matrix Matrice (int[hauteur][largeur]) à cloner.
     * @return Une nouvelle matrice contenant une copie des valeurs,
     *         ou null si la matrice d'entrée est null.
     */
    public static int[][] cloneMatrix(int[][] matrix) {
        if (matrix == null) return null;
        int height = matrix.length;
        // Gérer le cas d'une matrice avec 0 ligne mais potentiellement non nulle
        if (height == 0) return new int[0][];
        int width = matrix[0].length; // Largeur de la première ligne (suppose matrice rectangulaire)

        int[][] copy = new int[height][width];
        for (int y = 0; y < height; y++) {
            // Vérifier que la ligne actuelle n'est pas null et a la bonne largeur
            // (sécurité pour matrices non rectangulaires, bien que non attendu ici)
            if (matrix[y] == null || matrix[y].length != width) {
                System.err.println("Warning [cloneMatrix]: Matrice non rectangulaire détectée à la ligne " + y);
                // Décider comment gérer: retourner null, lancer une exception, ou copier quand même ?
                // Pour l'instant, on continue en espérant que ça n'arrive pas.
                // On pourrait aussi ajuster la largeur de la copie pour cette ligne spécifique:
                // copy[y] = new int[matrix[y] == null ? 0 : matrix[y].length];
                // width = copy[y].length; // Ajuster la largeur pour arraycopy
            }
            // Utiliser System.arraycopy pour copier chaque ligne (plus performant)
            System.arraycopy(matrix[y], 0, copy[y], 0, width);
        }
        return copy;
    }
    // --- FIN AJOUT ---

    /**
     * Convertit une CImage (NG ou RGB) en une matrice de niveaux de gris int[y][x].
     * Si l'image source est en couleur, elle est convertie en utilisant la formule de luminance standard.
     *
     * @param cImage L'image source (CImageNG ou CImageRGB).
     * @return Une matrice int[hauteur][largeur] représentant les niveaux de gris, 
     *         ou null si l'image d'entrée est invalide.
     */
    public static int[][] imageToGrayMatrix(CImage cImage) {
        if (cImage == null) {
            System.err.println("ImageUtils.imageToGrayMatrix: Input CImage is null.");
            return null;
        }
        BufferedImage bufferedImage = cImage.getImage();
        if (bufferedImage == null) {
            System.err.println("ImageUtils.imageToGrayMatrix: BufferedImage inside CImage is null.");
            return null;
        }

        int width = cImage.getLargeur();
        int height = cImage.getHauteur();

        if (width <= 0 || height <= 0) {
             System.err.println("ImageUtils.imageToGrayMatrix: Invalid image dimensions (" + width + "x" + height + ").");
            return null;
        }

        // Notre matrice utilise la convention [y][x] (hauteur, largeur)
        int[][] matrix = new int[height][width]; 

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = bufferedImage.getRGB(x, y);

                // Extraire les composantes R, G, B
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculer la luminance standard
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // Assurer que la valeur est dans [0, 255] (clamping)
                gray = Math.max(0, Math.min(255, gray));

                matrix[y][x] = gray; // Stocker dans notre convention [y][x]
            }
        }

        return matrix;
    }

    /**
     * Convertit une matrice de niveaux de gris int[y][x] en un objet CImageNG.
     * Cette méthode gère la transposition nécessaire car CImageNG utilise potentiellement
     * une matrice [x][y] en interne via son constructeur/setMatrice.
     *
     * @param matrix La matrice de niveaux de gris int[hauteur][largeur] (convention [y][x]).
     *               Les valeurs doivent être idéalement entre 0 et 255.
     * @return Un nouvel objet CImageNG, ou null en cas d'erreur ou si la matrice d'entrée est invalide.
     */
    public static CImageNG matrixToCImageNG(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            System.err.println("ImageUtils.matrixToCImageNG: Input matrix is null or empty.");
            return null;
        }

        int height = matrix.length;    // Nombre de lignes (y)
        int width = matrix[0].length; // Nombre de colonnes (x)

        // CImageNG(int[][] matrice) attend une matrice [largeur][hauteur] ([x][y])
        int[][] transposedMatrix = new int[width][height]; 

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Lire depuis notre convention [y][x]
                int gray = matrix[y][x]; 
                
                // Assurer que la valeur est dans [0, 255] (clamping)
                gray = Math.max(0, Math.min(255, gray));
                
                // Écrire dans la convention [x][y] pour CImageNG
                transposedMatrix[x][y] = gray; 
            }
        }

        try {
            // Créer l'objet CImageNG avec la matrice transposée
            return new CImageNG(transposedMatrix);
        } catch (CImageNGException e) {
            System.err.println("ImageUtils.matrixToCImageNG: Failed to create CImageNG - " + e.getMessage());
            // e.printStackTrace(); // Décommenter pour plus de détails si nécessaire
            return null;
        }
    }
    
    // --- Autres méthodes utilitaires pourront être ajoutées ici ---
    // Par exemple : cloner une matrice int[][], etc.

}