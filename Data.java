package benders;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class Data {
	int SourcesSize;    //供应点数量
	int DemandsSize;	//需求点数量
	double []supply;	//供应参数
	double []demand;	//需求参数
	double [][]c;		//供应单位资源的花费
	double [][]fixed_c;	//固定花费
	double[][] M;		//对于文章中Mij
	//读入数据
	public void read_data(String path) throws Exception {
		String line = null;
		String[] substr = null;
		Scanner cin = new Scanner(new BufferedReader(new FileReader(path)));
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		SourcesSize = Integer.parseInt(substr[0]);
		DemandsSize = Integer.parseInt(substr[1]);
		supply = new double[SourcesSize];
		demand = new double[DemandsSize];
		c = new double[SourcesSize][DemandsSize];
		fixed_c = new double[SourcesSize][DemandsSize];
		M = new double[SourcesSize][DemandsSize];
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		for (int i = 0; i < SourcesSize; i++) {
			supply[i] = Integer.parseInt(substr[i]);
		}
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		for (int i = 0; i < DemandsSize; i++) {
			demand[i] = Integer.parseInt(substr[i]);
		}
		line = cin.nextLine();
		for (int i = 0; i < SourcesSize; i++) {
			line = cin.nextLine();
			line.trim();
			substr = line.split(("\\s+"));
			for (int j = 0; j < DemandsSize; j++) {
				c[i][j] = Integer.parseInt(substr[j]);
			}
		}
		
		line = cin.nextLine();
		for (int i = 0; i < SourcesSize; i++) {
			line = cin.nextLine();
			line.trim();
			substr = line.split(("\\s+"));
			for (int j = 0; j < DemandsSize; j++) {
				fixed_c[i][j] = Integer.parseInt(substr[j]);
			}
		}
		cin.close();
		//设置M参数
		 for (int i = 0; i < SourcesSize; i++) {
			 for (int j = 0; j < DemandsSize; j++) {
			 M[i][j] = Math.min(supply[i], demand[j]);
			 }
		 }
	}
}
