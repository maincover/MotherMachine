package Threshold;

import ij.process.ImageProcessor;

public class Otsu_Threshold_OpenCV {


	public static int otsuThreshold(ImageProcessor ip)
	{
		int width = ip.getWidth();
		int height = ip.getHeight();	
		int GrayScale = 256;
		int[] pixelCount = new int[GrayScale];
		float[] pixelPro = new float[GrayScale];
		int i, j, pixelSum = width * height, threshold = 0;
		//Byte[] data = (Byte[])ip.getPixels();
		//ͳ�ƻҶȼ���ÿ������������ͼ���еĸ���

		for(i = 0; i < height; i++)
		{
			for(j = 0;j < width;j++)
			{
				pixelCount[ip.get(i, j)]++;
			}

		}

		//����ÿ������������ͼ���еı���
		for(i = 0; i < GrayScale; i++)
		{
			pixelPro[i] = (float)pixelCount[i] / pixelSum;
		}

		//�����Ҷȼ�[0,255]
		float w0, w1, u0tmp, u1tmp, u0, u1, u;
		double deltaTmp, deltaMax = 0;
		for(i = 0; i < GrayScale; i++)
		{
			w0 = w1 = u0tmp = u1tmp = u0 = u1 = u = 0;
			deltaTmp = 0;
			for(j = 0; j < GrayScale; j++)
			{
				if(j <= i)   //��������
				{
					w0 += pixelPro[j];
					u0tmp += j * pixelPro[j];
				}
				else   //ǰ������
				{
					w1 += pixelPro[j];
					u1tmp += j * pixelPro[j];
				}
			}
			u0 = u0tmp / w0;
			u1 = u1tmp / w1;
			u = u0tmp + u1tmp;
			deltaTmp = w0 * Math.pow((u0 - u), 2) + w1 * Math.pow((u1 - u), 2);
			if(deltaTmp > deltaMax)
			{
				deltaMax = deltaTmp;
				threshold = i;
			}
		}
		return threshold;
	}

}
