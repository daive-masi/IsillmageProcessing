package test; // Assure-toi que ce fichier est bien dans le package 'test'

import CImage.*;
import CImage.Exceptions.*;
import ImageProcessing.Core.ImageUtils;
import ImageProcessing.Core.PaddingUtils; // <-- Importer la nouvelle classe
import ImageProcessing.Core.BorderMode;  // <-- Importer l'enum BorderMode
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import ImageProcessing.Lineaire.FiltrageLinaireGlobal;
import ImageProcessing.Lineaire.FiltrageLineaireLocal;

public class ManualTester {

    public static void main(String[] args) {
        System.out.println("--- Début du Test Manuel ---");

        // --- Étape 1: Charger une image de test ---
        CImageRGB originalCImage = null;
        String inputImagePath = "lena.png"; // CHANGE CECI si nécessaire
        try {
            File inputFile = new File(inputImagePath);
            if (!inputFile.exists()) {
                System.err.println("Erreur : Le fichier image d'entrée n'existe pas : " + inputImagePath);
                System.err.println("Vérifie le chemin et assure-toi que l'image est à la racine du projet.");
                System.out.println("--- Fin du Test Manuel (Échec Chargement) ---");
                return;
            }
            originalCImage = new CImageRGB(inputFile);
            System.out.println("Image chargée : " + inputImagePath + " (" + originalCImage.getLargeur() + "x" + originalCImage.getHauteur() + ")");
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'image : " + e.getMessage());
            e.printStackTrace();
            System.out.println("--- Fin du Test Manuel (Échec Chargement) ---");
            return;
        } catch (Exception e) { // Catcher autres exceptions possibles de CImageRGB
            System.err.println("Erreur inattendue lors de la création de CImageRGB : " + e.getMessage());
            e.printStackTrace();
            System.out.println("--- Fin du Test Manuel (Échec Chargement) ---");
            return;
        }


        // --- Étape 2: Convertir CImage -> int[y][x] (avec luminance) ---
        System.out.println("\nConversion CImage -> int[y][x] (luminance)...");
        int[][] grayMatrix = ImageUtils.imageToGrayMatrix(originalCImage);

        if (grayMatrix == null) {
            System.err.println("Erreur : La conversion imageToGrayMatrix a retourné null.");
            System.out.println("--- Fin du Test Manuel (Échec Conversion 1) ---");
            return;
        }
        int height = grayMatrix.length;
        int width = grayMatrix[0].length;
        System.out.println("Matrice obtenue : " + height + " lignes (y) x " + width + " colonnes (x)");
        // Affichage de quelques pixels (optionnel)
        // if (height > 10 && width > 10) {
        //     System.out.println("Quelques valeurs de la matrice [y][x]:");
        //     System.out.println("  Pixel [0][0]: " + grayMatrix[0][0]);
        //     System.out.println("  Pixel [10][10]: " + grayMatrix[10][10]);
        // }

        // --- Étape 3: Convertir int[y][x] -> CImageNG ---
        System.out.println("\nConversion int[y][x] -> CImageNG...");
        CImageNG resultCImage = ImageUtils.matrixToCImageNG(grayMatrix);

        if (resultCImage == null) {
            System.err.println("Erreur : La conversion matrixToCImageNG a retourné null.");
            System.out.println("--- Fin du Test Manuel (Échec Conversion 2) ---");
            return;
        }
        System.out.println("CImageNG résultat obtenue : (" + resultCImage.getLargeur() + "x" + resultCImage.getHauteur() + ")");

        // --- Étape 4: Sauvegarder l'image résultat pour vérification manuelle ---
        String outputImagePath = "lena_test_output.png";
        System.out.println("\nSauvegarde de l'image résultat (Conversion) : " + outputImagePath);
        try {
            resultCImage.enregistreFormatPNG(new File(outputImagePath));
            System.out.println("Image de conversion sauvegardée avec succès !");
            System.out.println(">>> ACTION REQUISE : Ouvre '" + outputImagePath + "' et vérifie qu'elle est correcte.");

        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de l'image résultat : " + e.getMessage());
            e.printStackTrace();
        }

        // --- Étape 5: Tester PaddingUtils ---
        System.out.println("\n--- Début Tests PaddingUtils ---");

        // Créer une petite matrice de test
        int[][] smallMatrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        System.out.println("Matrice originale pour test padding (3x3):");
        System.out.println(Arrays.deepToString(smallMatrix).replace("], ", "]\n ")); // Affiche joliment

        int padSize = 1; // Padding de 1 pixel (pour un masque 3x3 par exemple)
        System.out.println("\nTest avec padSize = " + padSize + ":");

        // Test Mode ZERO
        System.out.println("\nMode: ZERO");
        int[][] paddedZero = PaddingUtils.padImage(smallMatrix, padSize, BorderMode.ZERO);
        if (paddedZero != null) {
            System.out.println(Arrays.deepToString(paddedZero).replace("], ", "]\n "));
        } else {
            System.out.println("Erreur pendant le padding ZERO.");
        }

        // Test Mode REPLICATE
        System.out.println("\nMode: REPLICATE");
        int[][] paddedReplicate = PaddingUtils.padImage(smallMatrix, padSize, BorderMode.REPLICATE);
        if (paddedReplicate != null) {
            System.out.println(Arrays.deepToString(paddedReplicate).replace("], ", "]\n "));
        } else {
            System.out.println("Erreur pendant le padding REPLICATE.");
        }

        // Test Mode MIRROR
        System.out.println("\nMode: MIRROR");
        int[][] paddedMirror = PaddingUtils.padImage(smallMatrix, padSize, BorderMode.MIRROR);
        if (paddedMirror != null) {
            System.out.println(Arrays.deepToString(paddedMirror).replace("], ", "]\n "));
        } else {
            System.out.println("Erreur pendant le padding MIRROR.");
        }

        System.out.println("\n--- Fin Tests PaddingUtils ---");

// --- Étape 6: Tester FiltrageLineaireLocal ---
        System.out.println("\n--- Début Tests FiltrageLineaireLocal ---");

        if (grayMatrix != null) { // Réutiliser la matrice de l'image chargée
            // Test filtreMoyenneur
            int tailleMoyenneur = 3; // Masque 3x3
            System.out.println("\nTest filtreMoyenneur avec tailleMasque = " + tailleMoyenneur + "...");
            int[][] imageMoyenne = FiltrageLineaireLocal.filtreMoyenneur(grayMatrix, tailleMoyenneur);
            if (imageMoyenne != null) {
                CImageNG imgMoyenneNG = ImageUtils.matrixToCImageNG(imageMoyenne);
                if (imgMoyenneNG != null) {
                    try {
                        String moyOutputPath = "lena_moyenneur3x3_output.png";
                        imgMoyenneNG.enregistreFormatPNG(new File(moyOutputPath));
                        System.out.println("Image Moyenneur sauvegardée : " + moyOutputPath);
                        System.out.println(">>>" +
                                " ACTION REQUISE : Vérifie '" + moyOutputPath + "', elle doit être floue.");
                    } catch (IOException e) { // Catcher aussi CImageNGException possible
                        System.err.println("Erreur sauvegarde image moyenneur: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Erreur pendant filtreMoyenneur.");
            }

            // Test filtreMasqueConvolution (Exemple: Détection de contours simple - Laplacien)
            System.out.println("\nTest filtreMasqueConvolution (Laplacien 4)...");
            double[][] masqueLaplacien4 = {
                    { 0,  1,  0 },
                    { 1, -4,  1 },
                    { 0,  1,  0 }
            };
            int[][] imageLaplacien = FiltrageLineaireLocal.filtreMasqueConvolution(grayMatrix, masqueLaplacien4);
            if (imageLaplacien != null) {
                CImageNG imgLaplacienNG = ImageUtils.matrixToCImageNG(imageLaplacien);
                if (imgLaplacienNG != null) {
                    try {
                        String lapOutputPath = "lena_laplacien4_output.png";
                        imgLaplacienNG.enregistreFormatPNG(new File(lapOutputPath));
                        System.out.println("Image Laplacien sauvegardée : " + lapOutputPath);
                        System.out.println(">>> ACTION REQUISE : Vérifie '" + lapOutputPath + "', elle doit montrer les contours.");
                    } catch (IOException e) {
                        System.err.println("Erreur sauvegarde image laplacien: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Erreur pendant filtreMasqueConvolution (Laplacien).");
            }

        } else {
            System.out.println("Skipping FiltrageLineaireLocal tests car grayMatrix est null.");
        }
        System.out.println("\n--- Fin Tests FiltrageLineaireLocal ---");
        // Dans ManualTester.java, après les tests précédents :

// --- Étape 7: Tester FiltrageLinaireGlobal ---
        System.out.println("\n--- Début Tests FiltrageLinaireGlobal ---");

        if (grayMatrix != null) {
            int hauteurImg = grayMatrix.length;
            int largeurImg = grayMatrix[0].length;

            // Test Passe-Bas Idéal
            int freqCoupurePB = 30; // Exemple de fréquence de coupure (à ajuster)
            System.out.println("\nTest filtrePasseBasIdeal avec freqCoupure = " + freqCoupurePB + "...");
            int[][] imgPasseBas = FiltrageLinaireGlobal.filtrePasseBasIdeal(grayMatrix, freqCoupurePB);
            if (imgPasseBas != null) {
                CImageNG imgPBNG = ImageUtils.matrixToCImageNG(imgPasseBas);
                if (imgPBNG != null) {
                    try {
                        String pbOutputPath = "lena_passebas_ideal_" + freqCoupurePB + "_output.png";
                        imgPBNG.enregistreFormatPNG(new File(pbOutputPath));
                        System.out.println("Image Passe-Bas Idéal sauvegardée : " + pbOutputPath);
                        System.out.println(">>> ACTION REQUISE : Vérifie '" + pbOutputPath + "', elle doit être floue (artefacts possibles).");
                    } catch (IOException e) {
                        System.err.println("Erreur sauvegarde image Passe-Bas: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Erreur pendant filtrePasseBasIdeal.");
            }

            // Test Passe-Haut Butterworth
            int freqCoupurePH = 20; // Exemple (à ajuster)
            int ordrePH = 2;      // Ordre du filtre Butterworth
            System.out.println("\nTest filtrePasseHautButterworth avec freqCoupure = " + freqCoupurePH + ", ordre = " + ordrePH + "...");
            int[][] imgPasseHaut = FiltrageLinaireGlobal.filtrePasseHautButterworth(grayMatrix, freqCoupurePH, ordrePH);
            if (imgPasseHaut != null) {
                CImageNG imgPHNG = ImageUtils.matrixToCImageNG(imgPasseHaut);
                if (imgPHNG != null) {
                    try {
                        String phOutputPath = "lena_passehaut_butter_" + freqCoupurePH + "_ord" + ordrePH + "_output.png";
                        imgPHNG.enregistreFormatPNG(new File(phOutputPath));
                        System.out.println("Image Passe-Haut Butterworth sauvegardée : " + phOutputPath);
                        System.out.println(">>> ACTION REQUISE : Vérifie '" + phOutputPath + "', elle doit montrer les contours/détails rehaussés.");
                    } catch (IOException e) {
                        System.err.println("Erreur sauvegarde image Passe-Haut: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Erreur pendant filtrePasseHautButterworth.");
            }

        } else {
            System.out.println("Skipping FiltrageLinaireGlobal tests car grayMatrix est null.");
        }
        System.out.println("\n--- Fin Tests FiltrageLinaireGlobal ---");

        System.out.println("\n--- Fin du Test Manuel ---");
    }

}