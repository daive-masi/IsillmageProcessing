package ImageProcessing.Seuillage;

import ImageProcessing.Histogramme.Histogramme; // Nécessaire pour Otsu

/**
 * Contient des méthodes statiques pour la segmentation d'images par seuillage.
 */
public class Seuillage {

    /**
     * Réalise un seuillage simple (binarisation) de l'image.
     * Les pixels <= seuil deviennent 0 (noir), les pixels > seuil deviennent 255 (blanc).
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @param seuil La valeur de seuil (typiquement entre 0 et 255).
     * @return Une nouvelle image binaire (int[hauteur][largeur] avec valeurs 0 ou 255).
     *         Retourne null si l'image d'entrée est invalide.
     *
     * @technique Seuillage Simple (Binarisation).
     */
    public static int[][] seuillageSimple(int[][] image, int seuil) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;

        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] resultat = new int[hauteur][largeur];

        // S'assurer que le seuil est dans une plage raisonnable
        // (on pourrait débattre si on autorise <0 ou >255, mais restons simple)
        // int seuilValide = Math.max(0, Math.min(255, seuil));

        System.out.println("Application Seuillage Simple (seuil=" + seuil + ")...");

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Appliquer la condition de seuillage
                resultat[y][x] = (image[y][x] > seuil) ? 255 : 0;
            }
        }
        return resultat;
    }

    /**
     * Réalise un seuillage multiple avec deux seuils.
     * Crée une image avec 3 niveaux de gris distincts.
     * - Pixels <= seuil1      -> niveauBas (ex: 0)
     * - seuil1 < Pixels <= seuil2 -> niveauMoyen (ex: 128)
     * - Pixels > seuil2      -> niveauHaut (ex: 255)
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @param seuil1 Le premier seuil (inférieur).
     * @param seuil2 Le deuxième seuil (supérieur). Doit être >= seuil1.
     * @return Une nouvelle image avec 3 niveaux (int[hauteur][largeur]).
     *         Retourne null si l'image d'entrée est invalide ou si seuil1 > seuil2.
     *
     * @technique Seuillage Multiple (Double Seuil).
     */
    public static int[][] seuillageDouble(int[][] image, int seuil1, int seuil2) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        if (seuil1 > seuil2) {
            System.err.println("Erreur [seuillageDouble]: seuil1 doit être <= seuil2.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] resultat = new int[hauteur][largeur];

        // Définir les niveaux de sortie (on peut les choisir)
        final int NIVEAU_BAS = 0;
        final int NIVEAU_MOYEN = 128;
        final int NIVEAU_HAUT = 255;

        System.out.println("Application Seuillage Double (seuils=" + seuil1 + ", " + seuil2 + ")...");

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int pixel = image[y][x];
                if (pixel <= seuil1) {
                    resultat[y][x] = NIVEAU_BAS;
                } else if (pixel <= seuil2) { // pixel > seuil1 ET pixel <= seuil2
                    resultat[y][x] = NIVEAU_MOYEN;
                } else { // pixel > seuil2
                    resultat[y][x] = NIVEAU_HAUT;
                }
            }
        }
        return resultat;
    }

    /**
     * Réalise un seuillage automatique en utilisant l'algorithme d'Otsu
     * pour déterminer le seuil optimal qui minimise la variance intra-classe.
     * Applique ensuite un seuillage simple avec ce seuil.
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @return Une nouvelle image binaire (int[hauteur][largeur] avec valeurs 0 ou 255).
     *         Retourne null si l'image d'entrée est invalide.
     *
     * @technique Seuillage Automatique, Méthode d'Otsu. Section 1.6.4 des notes.
     */
    public static int[][] seuillageAutomatique(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;

        // 1. Calculer l'histogramme normalisé (p(i))
        int[] hist = Histogramme.Histogramme256(image);
        if (hist == null) return null;

        long nbPixelsTotal = 0;
        for (int count : hist) {
            nbPixelsTotal += count;
        }
        if (nbPixelsTotal == 0) return seuillageSimple(image, 127); // Image vide? Retourne seuil par défaut

        double[] histNorm = new double[256];
        for (int i = 0; i < 256; i++) {
            histNorm[i] = (double) hist[i] / nbPixelsTotal;
        }

        // 2. Itérer sur tous les seuils possibles (k de 0 à 255) pour trouver le meilleur
        double maxVarianceInterClasse = -1.0;
        int seuilOptimal = 0;

        for (int k = 0; k < 256; k++) {
            // Calculer P1(k) : probabilité cumulée jusqu'à k (classe 1 = fond)
            double p1_k = 0.0;
            for (int i = 0; i <= k; i++) {
                p1_k += histNorm[i];
            }

            // Calculer P2(k) : probabilité cumulée après k (classe 2 = objet)
            double p2_k = 1.0 - p1_k; // Ou recalculer: sum(histNorm[i] for i=k+1 to 255)

            // Si P1(k) ou P2(k) est proche de zéro, ce seuil n'est pas bon
            // (tous les pixels seraient dans une seule classe)
            if (p1_k < 1e-9 || p2_k < 1e-9) {
                continue;
            }

            // Calculer m1(k) : moyenne des niveaux de gris jusqu'à k
            double m1_k = 0.0;
            for (int i = 0; i <= k; i++) {
                m1_k += i * histNorm[i];
            }
            m1_k /= p1_k; // Normaliser par la proba de la classe

            // Calculer m2(k) : moyenne des niveaux de gris après k
            double m2_k = 0.0;
            for (int i = k + 1; i < 256; i++) {
                m2_k += i * histNorm[i];
            }
            m2_k /= p2_k; // Normaliser par la proba de la classe

            // Calculer la variance inter-classe sigma_B^2(k)
            // Formule: P1(k) * P2(k) * (m1(k) - m2(k))^2
            double variance_k = p1_k * p2_k * Math.pow(m1_k - m2_k, 2);

            // Mettre à jour le seuil optimal si la variance actuelle est meilleure
            if (variance_k > maxVarianceInterClasse) {
                maxVarianceInterClasse = variance_k;
                seuilOptimal = k;
            }
        }

        System.out.println("Application Seuillage Automatique (Otsu)... Seuil optimal trouvé = " + seuilOptimal);

        // 3. Appliquer un seuillage simple avec le seuil optimal trouvé
        return seuillageSimple(image, seuilOptimal);
    }
}