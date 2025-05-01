package ImageProcessing.NonLineaire;

import ImageProcessing.Core.PaddingUtils;
import ImageProcessing.Core.BorderMode;
import ImageProcessing.Core.ImageUtils; // <--- AJOUTER CET IMPORT ---
import java.util.Arrays; // Nécessaire pour Arrays.sort()

/**
 * Contient des méthodes statiques pour des opérations morphologiques complexes
 * et d'autres filtres non-linéaires.
 */
public class MorphoComplexe {

    /**
     * Applique un filtre médian à une image en niveaux de gris.
     * Chaque pixel est remplacé par la valeur médiane des pixels dans son voisinage carré.
     * Ce filtre est efficace pour réduire le bruit impulsionnel (sel et poivre)
     * tout en préservant relativement bien les contours.
     * Gère les bords par réplication.
     *
     * @param image        L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param tailleMasque La taille du voisinage carré (doit être impair >= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat du filtrage médian.
     * Retourne null si les entrées sont invalides.
     * @technique Filtrage Non-Linéaire, Filtre de Rang, Filtre Médian.
     */
    public static int[][] filtreMedian(int[][] image, int tailleMasque) {
        // --- Validation des entrées ---
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [filtreMedian]: L'image d'entrée est invalide.");
            return null;
        }
        if (tailleMasque <= 0) {
            System.err.println("Erreur [filtreMedian]: La taille du masque doit être positive.");
            return null;
        }
        if (tailleMasque % 2 == 0) {
            System.err.println("Erreur [filtreMedian]: La taille du masque doit être impaire.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int padSize = (tailleMasque - 1) / 2;

        // --- Padding ---
        int[][] imagePaddee = PaddingUtils.padImage(image, padSize, BorderMode.REPLICATE);
        if (imagePaddee == null) {
            System.err.println("Erreur [filtreMedian]: Échec lors du padding de l'image.");
            return null;
        }

        // Création de l'image résultat
        int[][] resultat = new int[hauteur][largeur];
        int nbVoisins = tailleMasque * tailleMasque;
        int[] voisins = new int[nbVoisins]; // Tableau pour stocker les voisins
        int indiceMilieu = nbVoisins / 2; // Indice de la médiane dans le tableau trié

        // --- Application du filtre médian ---
        // Itérer sur chaque pixel (y, x) de l'image *originale*
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {

                // 1. Collecter les valeurs des voisins
                int k = 0; // Index pour le tableau voisins
                for (int i = -padSize; i <= padSize; i++) {
                    for (int j = -padSize; j <= padSize; j++) {
                        voisins[k++] = imagePaddee[y + padSize + i][x + padSize + j];
                    }
                }

                // 2. Trier les voisins
                Arrays.sort(voisins);

                // 3. Extraire la médiane (valeur du milieu)
                int valeurMediane = voisins[indiceMilieu];

                // 4. Stocker la médiane dans l'image résultat
                resultat[y][x] = valeurMediane;
            }
        }
        return resultat;
    }

    /**
     * Réalise la dilatation géodésique d'une image 'marqueur' conditionnellement
     * à une image 'masque', répétée un nombre fixe de fois.
     * L'élément structurant pour la dilatation élémentaire est un carré 3x3 implicite.
     *
     * @param marqueur L'image de départ à dilater (int[hauteur][largeur]).
     * @param masque   L'image qui contraint la dilatation (int[hauteur][largeur]).
     *                 Doit avoir les mêmes dimensions que le marqueur. La condition
     *                 marqueur[y][x] <= masque[y][x] doit être respectée idéalement.
     * @param nbIter   Le nombre d'itérations de dilatation géodésique de taille 1 à appliquer (>= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de la dilatation géodésique,
     * ou null si les entrées sont invalides ou incompatibles.
     * @technique Morphologie Géodésique, Dilatation Géodésique Itérative.
     */
    public static int[][] dilatationGeodesique(int[][] marqueur, int[][] masque, int nbIter) {
        // --- Validation des entrées ---
        if (marqueur == null || masque == null || marqueur.length == 0 || masque.length == 0 ||
                marqueur[0].length == 0 || masque[0].length == 0) {
            System.err.println("Erreur [dilatationGeodesique]: Images marqueur ou masque invalides.");
            return null;
        }
        if (marqueur.length != masque.length || marqueur[0].length != masque[0].length) {
            System.err.println("Erreur [dilatationGeodesique]: Les dimensions des images marqueur et masque doivent être identiques.");
            return null;
        }
        if (nbIter < 1) {
            System.err.println("Erreur [dilatationGeodesique]: Le nombre d'itérations doit être >= 1.");
            return null;
        }

        // Commencer avec une copie du marqueur pour ne pas le modifier
        int[][] resultatCourant = ImageUtils.cloneMatrix(marqueur);
        if (resultatCourant == null) return null; // Échec du clonage

        System.out.println("Début Dilatation Géodésique (" + nbIter + " itérations)...");

        // Itérer nbIter fois
        for (int i = 0; i < nbIter; i++) {
            // 1. Dilater l'image courante avec un élément structurant 3x3
            //    On utilise la dilatation de MorphoElementaire
            int[][] dilatee = MorphoElementaire.dilatation(resultatCourant, 3);
            if (dilatee == null) {
                System.err.println("Erreur [dilatationGeodesique]: Échec dilatation élémentaire à l'itération " + (i + 1));
                return null; // Échec d'une étape intermédiaire
            }

            // 2. Prendre le minimum point par point avec le masque
            resultatCourant = minimumPointParPoint(dilatee, masque);
            if (resultatCourant == null) { // minimumPointParPoint gère la vérification des dimensions
                System.err.println("Erreur [dilatationGeodesique]: Échec minimum point par point à l'itération " + (i + 1));
                return null;
            }

            // Optionnel: Log de progression
            // if ((i + 1) % 10 == 0) {
            //     System.out.println("Itération " + (i + 1) + " terminée.");
            // }
        }

        System.out.println("Dilatation Géodésique terminée.");
        return resultatCourant;
    }


    /**
     * Réalise la reconstruction géodésique d'une image 'marqueur'
     * conditionnellement à une image 'masque' par dilatation.
     * La reconstruction est obtenue en appliquant la dilatation géodésique
     * de taille 1 jusqu'à ce que l'image n'évolue plus (convergence).
     * L'élément structurant pour la dilatation élémentaire est un carré 3x3 implicite.
     *
     * @param marqueur L'image de départ (int[hauteur][largeur]).
     * @param masque   L'image qui contraint la reconstruction (int[hauteur][largeur]).
     *                 Doit avoir les mêmes dimensions et marqueur[y][x] <= masque[y][x].
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de la reconstruction,
     * ou null si les entrées sont invalides ou incompatibles.
     * @technique Morphologie Géodésique, Reconstruction par Dilatation.
     */
    public static int[][] reconstructionGeodesique(int[][] marqueur, int[][] masque) {
        // --- Validation des entrées (similaire à dilatationGeodesique) ---
        if (marqueur == null || masque == null || marqueur.length == 0 || masque.length == 0 ||
                marqueur[0].length == 0 || masque[0].length == 0) {
            System.err.println("Erreur [reconstructionGeodesique]: Images marqueur ou masque invalides.");
            return null;
        }
        if (marqueur.length != masque.length || marqueur[0].length != masque[0].length) {
            System.err.println("Erreur [reconstructionGeodesique]: Les dimensions des images marqueur et masque doivent être identiques.");
            return null;
        }

        // Commencer avec une copie du marqueur
        int[][] resultatPrecedent = null; // Pour détecter la convergence
        int[][] resultatCourant = ImageUtils.cloneMatrix(marqueur);
        if (resultatCourant == null) return null;

        System.out.println("Début Reconstruction Géodésique...");
        int iteration = 0;
        final int MAX_ITERATIONS = 10000; // Sécurité pour éviter boucle infinie

        // Itérer jusqu'à convergence ou limite max
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            resultatPrecedent = resultatCourant; // Garder la version précédente

            // Appliquer UNE itération de dilatation géodésique de taille 1
            int[][] dilatee = MorphoElementaire.dilatation(resultatPrecedent, 3);
            if (dilatee == null) {
                System.err.println("Erreur [reconstructionGeodesique]: Échec dilatation élémentaire à l'itération " + iteration);
                return null;
            }
            resultatCourant = minimumPointParPoint(dilatee, masque);
            if (resultatCourant == null) {
                System.err.println("Erreur [reconstructionGeodesique]: Échec minimum point par point à l'itération " + iteration);
                return null;
            }

            // Vérifier la convergence: l'image a-t-elle changé ?
            if (Arrays.deepEquals(resultatCourant, resultatPrecedent)) {
                System.out.println("Reconstruction Géodésique convergée après " + iteration + " itérations.");
                return resultatCourant; // Convergence atteinte !
            }
            // Optionnel: Log de progression
            // if (iteration % 50 == 0) {
            //    System.out.println("Itération de reconstruction " + iteration + "...");
            // }
        }

        // Si on sort de la boucle sans converger (très improbable avec des images finies)
        System.err.println("Erreur [reconstructionGeodesique]: Convergence non atteinte après " + MAX_ITERATIONS + " itérations.");
        return resultatCourant; // Retourner le dernier état calculé
    }
    // --- Fonctions Utilitaires Privées ---

    /**
     * Calcule le minimum point par point entre deux images (matrices int[][]).
     * Les images doivent avoir les mêmes dimensions.
     * @param img1 Première image.
     * @param img2 Deuxième image.
     * @return Une nouvelle image contenant le minimum point par point, ou null si incompatible.
     */
    private static int[][] minimumPointParPoint(int[][] img1, int[][] img2) {
        if (img1 == null || img2 == null || img1.length != img2.length || img1[0].length != img2[0].length) {
            System.err.println("Erreur [minimumPointParPoint]: Images incompatibles pour le minimum.");
            return null;
        }
        int height = img1.length;
        int width = img1[0].length;
        int[][] resultat = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                resultat[y][x] = Math.min(img1[y][x], img2[y][x]);
            }
        }
        return resultat;
    }

}

