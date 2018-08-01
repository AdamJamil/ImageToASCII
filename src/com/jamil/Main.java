package com.jamil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main
{
    int detail = 600;

    File imageFile = new File("res/DSC_0349.png");
    BufferedImage image = null;
    BufferedImage alphabetImage = null;
    BufferedImage drawnLettersImage = null;
    BufferedImage outputImage;
    BufferedImage coloredOutputImage;
    int[][][] alphabet = new int[97][6][11];
    int[][] greyscale;
    int[][] sobel;

    Main()
    {
        try
        {
            image = ImageIO.read(imageFile);
            greyscale = new int[image.getWidth()][image.getHeight()];

            drawnLettersImage = ImageIO.read(new File("res/drawnalphabet.png"));

            alphabetImage = ImageIO.read(new File("res/alphabet.png"));

            for (int i = 0; i < 97; i++)
                for (int j = 0; j < 6; j++)
                    for (int k = 0; k < 11; k++)
                        alphabet[i][j][k] = alphabetImage.getRGB((i * 6) + j, k) & 0x1;

            //greyscale
            for (int i = 0; i < image.getWidth(); i++)
                for (int j = 0; j < image.getHeight(); j++)
                {
                    int color = image.getRGB(i, j);
                    greyscale[i][j] = (((color >> 16) & 0xFF) + ((color >> 8) & 0xFF) + (color & 0xFF)) / 3;
                }

            //round up to nearest multiple of 6 after getting rid of borders
            int sobelWidth = image.getWidth() - 2;
            if (sobelWidth % 6 != 0)
            {
                sobelWidth /= 6;
                sobelWidth++;
                sobelWidth *= 6;
            }

            int sobelHeight = image.getHeight() - 2;
            if (sobelHeight% 11 != 0)
            {
                sobelHeight /= 11;
                sobelHeight++;
                sobelHeight *= 11;
            }

            sobel = new int[sobelWidth][sobelHeight];

            int max = 0;

            //sobel filter
            for (int i = 1; i < image.getWidth() - 1; i++)
            {
                int a = greyscale[i - 1][0];
                int b = greyscale[i - 1][1];
                int c = greyscale[i - 1][2];

                int d = greyscale[i][0];
                int e = greyscale[i][2];

                int f = greyscale[i + 1][0];
                int g = greyscale[i + 1][1];
                int h = greyscale[i + 1][2];

                for (int j = 1; j < image.getHeight() - 1; j++)
                {
                    int Gx = a + (2 * d) + f - c - (2 * e) - h;
                    Gx *= Gx;
                    int Gy = a + 2 * b + c - f - 2 * g - h;
                    Gy *= Gy;

                    int color = Gx + Gy;

                    if (color > max)
                        max = color;

                    sobel[i - 1][j - 1] = color;

                    if (j != image.getHeight() - 2)
                    {
                        a = b;
                        b = c;
                        f = g;
                        g = h;
                        d = greyscale[i][j];
                        c = greyscale[i - 1][j + 2];
                        e = greyscale[i][j + 2];
                        h = greyscale[i + 1][j + 2];
                    }
                }
            }

            for (int i = 1; i < sobel.length - 1; i++)
                for (int j = 1; j < sobel[i].length - 1; j++)
                    if (sobel[i - 1][j - 1] * detail > max)
                        sobel[i - 1][j - 1] = 1;
                    else
                        sobel[i - 1][j - 1] = 0;

            outputImage = new BufferedImage(sobelWidth, sobelHeight, (image.getType() == 0) ? 5 : image.getType());
            coloredOutputImage = new BufferedImage(sobelWidth, sobelHeight, (image.getType() == 0) ? 5 : image.getType());

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
                    }

                    for (int l = 0; l < 6; l++)
                        for (int m = 0; m < 11; m++)
                        {
                            int color = drawnLettersImage.getRGB(index * 6 + l, m);
                            outputImage.setRGB(j + l, i + m, color);
                            if ((color & 0xFFFFFF) == (43 << 16) + (43 << 8) + 43)
                                coloredOutputImage.setRGB(j + l, i + m, 0xFF000000);
                            else
                            {
                                if (image.getWidth() > j + l && image.getHeight() > i + m)
                                    coloredOutputImage.setRGB(j + l, i + m, image.getRGB(j + l, i + m));
                                else
                                    coloredOutputImage.setRGB(j + l, i + m, 0xFF000000);
                            }
                        }
                }
            }

            ImageIO.write(outputImage, "png", new File("out.png"));
            ImageIO.write(coloredOutputImage, "png", new File("colorout.png"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        new Main();
    }
}
