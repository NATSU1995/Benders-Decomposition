package benders;
import ilog.cplex.IloCplex;

public class Solution {
	public double cost;  // 总花费
	public double[][] ship;  //运载量
	public double[][] link_y;//状态指标，对应y
	public IloCplex.CplexStatus status;  // status returned by CPLEX
	//输出ship的值
	public void print_ship() {
		for (int i = 0; i < ship.length; i++) {
			for (int j = 0; j < ship[i].length; j++) {
				System.out.printf("\t%d -> %d: %f", i, j, ship[i][j],"  ");
			}
			System.out.println();
		}
	}
}
