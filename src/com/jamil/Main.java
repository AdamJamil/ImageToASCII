package com.jamil;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;

public class Main
{
    int detail = 4000;

    private static final int width = 640;
    private static final int height = 480;
    private static final int sobelWidth = 642;
    private static final int sobelHeight = 484;

    BufferedImage alphabetImage = null;
    BufferedImage drawnLettersImage = null;
    BufferedImage outputImage;
    boolean[][][] alphabet = new boolean[97][6][11];
    int[][] drawnAlphabet = new int[97][66];
    byte[] rawGreyscale = new byte[width * height * 3];
    int[] greyscale = new int[width * height];
    boolean[][] sobel = new boolean[sobelWidth][sobelHeight];

    int skipCounter = 0;

    Main()
    {
        try
        {
            drawnLettersImage = ImageIO.read(new File("res/drawnalphabet.png"));

            for (int i = 0; i < 97; i++)
                for (int j = 0; j < 6; j++)
                    for (int k = 0; k < 11; k++)
                        drawnAlphabet[i][j * 11 + k] = drawnLettersImage.getRGB((i * 6) + j, k);

            alphabetImage = ImageIO.read(new File("res/alphabet.png"));

            for (int i = 0; i < 97; i++)
                for (int j = 0; j < 6; j++)
                    for (int k = 0; k < 11; k++)
                        alphabet[i][j][k] = (alphabetImage.getRGB((i * 6) + j, k) & 0x1) == 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        // Open a webcam at a resolution close to 640x480
        Webcam webcam = UtilWebcamCapture.openDefault(640, 480);

        // Create the panel used to display the image and feature tracks
        ImagePanel gui = new ImagePanel();
        gui.setPreferredSize(new Dimension(sobelWidth, sobelHeight));

        ShowImages.showWindow(gui,"KLT Tracker",true);

        long time = System.nanoTime();
        int frames = 0;
        while(true)
        {
            gui.setBufferedImageSafe(getTextImage(webcam.getImage()));
            frames++;

            if (System.nanoTime() - time > 1000000000)
            {
                time = System.nanoTime();
                System.out.println(frames + " fps");
                frames = 0;
            }
        }
    }

    BufferedImage getTextImage(BufferedImage input)
    {
        rawGreyscale = ((DataBufferByte) input.getRaster().getDataBuffer()).getData();

        //greyscale
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                greyscale[i + width * j] = (rawGreyscale[3 * (i + width * j)] + rawGreyscale[3 * (i + width * j) + 1] + rawGreyscale[3 * (i + width * j) + 2]) / 3;

        //sobel filter
        for (int i = 1; i < width - 1; i++)
        {
            int a = greyscale[i - 1];
            int b = greyscale[i - 1 + width];
            int c = greyscale[i - 1 + width + width];

            int d = greyscale[i];
            int e = greyscale[i + width + width];

            int f = greyscale[i + 1];
            int g = greyscale[i + 1 + width];
            int h = greyscale[i + 1 + width + width];

            for (int j = 1; j < height - 1; j++)
            {
                int Gx = a + (2 * d) + f - c - (2 * e) - h;
                Gx *= Gx;
                int Gy = a + 2 * b + c - f - 2 * g - h;
                Gy *= Gy;

                int color = Gx + Gy;

                sobel[i - 1][j - 1] = color >= detail;

                if (j != height - 2)
                {
                    a = b;
                    b = c;
                    f = g;
                    g = h;
                    d = greyscale[i + width * j];
                    c = greyscale[(i - 1) + width * (j + 2)];
                    e = greyscale[i + width * (j + 2)];
                    h = greyscale[(i + 1) + width * (j + 2)];
                }
            }
        }

        outputImage = new BufferedImage(sobelWidth, sobelHeight, BufferedImage.TYPE_INT_RGB);
        int[] imageData = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < sobelHeight; i += 11)
        {
            for (int j = 0; j < sobelWidth; j += 6)
            {
                int maxScore = 0;
                int index = 0;
                for (int k = 0; k < 97; k++)
                {
                    int tempScore = 0;
                    for (int l = 0; l < 6; l++)
                        for (int m = 0; m < 11; m++)
                            if (sobel[j + l][i + m] == alphabet[k][l][m])
                                tempScore++;

                    if (tempScore > maxScore)
                    {
                        index = k;
                        maxScore = tempScore;
                    }

                    if (tempScore == 66)
                        break;
                }

                for (int l = 0; l < 6; l++)
                    for (int m = 0; m < 11; m++)
                        imageData[(i + m) * sobelWidth + j + l] = drawnAlphabet[index][l * 11 + m];
            }
        }

        return outputImage;
    }

    public static void main(String[] args)
    {
        new Main();
    }
}
