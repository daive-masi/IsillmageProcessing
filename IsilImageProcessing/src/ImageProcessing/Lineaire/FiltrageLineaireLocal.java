package ImageProcessing.Lineaire;

import ImageProcessing.Core.PaddingUtils;
import ImageProcessing.Core.BorderMode;
import java.util.Arrays; // Pour des messages d'erreur potentiels

/**
 * Contient des méthodes statiques pour le filtrage linéaire local (spatial) d'images.
 * Ces méthodes opèrent sur des voisinages de pixels en utilisant des masques de convolution.
 */
public class FiltrageLineaireLocal {

    /**
     * Réalise un filtrage local de l'image en utilisant un masque (noyau) de convolution.
     * La convolution est effectuée dans le domaine spatial.
     * Gère les bords de l'image en utilisant le padding par réplication (REPLICATE).
     *
     * @param image L'image d'entrée (matrice int[hauteur][largeur] avec convention [y][x]).
     *              Les valeurs sont supposées être entre 0 et 255.
     * @param masque Le masque (noyau) de convolution (matrice double[n][n] où n est impair).
     *               La somme des éléments du masque n'est pas nécessairement 1.
     * @return Une nouvelle image (matrice int[hauteur][largeur]) résultat de la convolution.
     *         Les valeurs sont clampées entre 0 et 255. Retourne null si les entrées sont invalides.
     *
     * @technique Convolution discrète 2D, Gestion des bords par Padding (REPLICATE).
     */
    public static int[][] filtreMasqueConvolution(int[][] image, double[][] masque) {
        // --- Validation des entrées ---
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [filtreMasqueConvolution]: L'image d'entrée est invalide.");
            return null;
        }
        if (masque == null || masque.length == 0 || masque[0].length == 0) {
            System.err.println("Erreur [filtreMasqueConvolution]: Le masque d'entrée est invalide.");
            return null;
        }
        if (masque.length != masque[0].length) {
            System.err.println("Erreur [filtreMasqueConvolution]: Le masque doit être carré.");
            return null;
        }
        if (masque.length % 2 == 0) {
            System.err.println("Erreur [filtreMasqueConvolution]: La taille du masque doit être impaire.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int tailleMasque = masque.length;
        int padSize = (tailleMasque - 1) / 2; // Taille du padding nécessaire de chaque côté

        // --- Préparation : Padding de l'image ---
        // Utilisation du mode REPLICATE pour gérer les bords, minimise les artefacts.
        int[][] imagePaddee = PaddingUtils.padImage(image, padSize, BorderMode.REPLICATE);
        if (imagePaddee == null) {
            System.err.println("Erreur [filtreMasqueConvolution]: Échec lors du padding de l'image.");
            return null;
        }

        // Création de l'image résultat (mêmes dimensions que l'originale)
        int[][] resultat = new int[hauteur][largeur];

        // --- Application de la convolution ---
        // Itérer sur chaque pixel (y, x) de l'image *originale*
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                double sommePonderee = 0.0;

                // Itérer sur le voisinage défini par le masque
                // i, j sont les décalages par rapport au centre (-padSize à +padSize)
                for (int i = -padSize; i <= padSize; i++) {
                    for (int j = -padSize; j <= padSize; j++) {
                        // Coordonnées du pixel voisin dans l'image *paddée*
                        int voisinY = y + padSize + i;
                        int voisinX = x + padSize + j;

                        // Coordonnées correspondantes dans le *masque*
                        int masqueY = i + padSize;
                        int masqueX = j + padSize;

                        // Accumuler la somme pondérée
                        sommePonderee += imagePaddee[voisinY][voisinX] * masque[masqueY][masqueX];
                    }
                }

                // --- Conversion et Clamping du résultat ---
                // Arrondir le résultat à l'entier le plus proche
                int valeurArrondie = (int) Math.round(sommePonderee);
                // Clamper (limiter) la valeur entre 0 et 255
                resultat[y][x] = Math.max(0, Math.min(255, valeurArrondie));
            }
        }

        return resultat;
    }

    /**
     * Réalise un filtrage moyenneur de l'image en utilisant un masque de convolution uniforme.
     * Chaque pixel est remplacé par la moyenne des pixels dans son voisinage.
     * C'est un cas particulier de filtre passe-bas.
     *
     * @param image L'image d'entrée (matrice int[hauteur][largeur] avec convention [y][x]).
     * @param tailleMasque La taille du voisinage carré (par exemple, 3 pour 3x3, 5 pour 5x5).
     *                     Doit être un entier positif impair.
     * @return Une nouvelle image (matrice int[hauteur][largeur]) résultat du filtrage moyenneur.
     *         Retourne null si les entrées sont invalides.
     *
     * @technique Filtre Moyenneur (Box Blur), implémenté via Convolution.
     */
    public static int[][] filtreMoyenneur(int[][] image, int tailleMasque) {
        // --- Validation des entrées ---
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [filtreMoyenneur]: L'image d'entrée est invalide.");
            return null;
        }
        if (tailleMasque <= 0) {
            System.err.println("Erreur [filtreMoyenneur]: La taille du masque doit être positive.");
            return null;
        }
        if (tailleMasque % 2 == 0) {
            System.err.println("Erreur [filtreMoyenneur]: La taille du masque doit être impaire.");
            return null;
        }

        // --- Création du masque moyenneur ---
        // Le masque est de taille [tailleMasque x tailleMasque]
        // Chaque poids est égal à 1 / (nombre total de pixels dans le masque)
        double poids = 1.0 / (tailleMasque * tailleMasque);
        double[][] masqueMoyenneur = new double[tailleMasque][tailleMasque];

        // Remplir le masque avec le poids calculé
        for (int i = 0; i < tailleMasque; i++) {
            for (int j = 0; j < tailleMasque; j++) {
                masqueMoyenneur[i][j] = poids;
            }
        }
        // Alternative plus rapide pour le remplissage :
        // for (double[] row : masqueMoyenneur) {
        //    Arrays.fill(row, poids);
        // }

        // --- Appel de la convolution générale ---
        // On réutilise la fonction de convolution générique avec le masque spécifique créé.
        return filtreMasqueConvolution(image, masqueMoyenneur);
    }
}