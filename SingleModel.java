package benders;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

//直接调用cplex求解固定消费的运输问题
public class SingleModel {
	private IloCplex cplex;
	private IloNumVar[][] x;// 对应MIP模型中的变量xij
	private IloNumVar[][] y;// 对应MIP模型中变量yij
	//建立cplex模型
	public void standardMIP(Data data) throws IloException {
		cplex = new IloCplex();
		 cplex.setOut(null);
		x = new IloNumVar[data.SourcesSize][data.DemandsSize];
		y = new IloNumVar[data.SourcesSize][data.DemandsSize];
		
		// 定义cplex变量x和y的数据类型及取值范围
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[i].length; j++) {
				x[i][j] = cplex.numVar(0, 1.0e5, IloNumVarType.Float, "x_" + i + "_" + j);
				y[i][j] = cplex.numVar(0, 1, IloNumVarType.Int, "y_" + i + "_" + j);
			}
		}

		IloNumExpr obj = cplex.numExpr();
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				obj = cplex.sum(obj,
						cplex.sum(cplex.prod(data.fixed_c[i][j], y[i][j]), cplex.prod(data.c[i][j], x[i][j])));
			}
		}
		cplex.addMinimize(obj, "total costs");// 目标函数
		//添加约束
		// supply
		for (int i = 0; i < data.SourcesSize; i++) {
			IloNumExpr expr1 = cplex.numExpr();
			for (int j = 0; j < data.DemandsSize; j++) {
				expr1 = cplex.sum(expr1, x[i][j]);
			}
			cplex.addLe(expr1, data.supply[i], "Supply_" + i);
		}
		// demand
		for (int i = 0; i < data.DemandsSize; i++) {
			IloNumExpr expr2 = cplex.numExpr();
			for (int j = 0; j < data.SourcesSize; j++) {
				expr2 = cplex.sum(expr2, x[j][i]);
			}
			cplex.addGe(expr2, data.demand[i], "Demand_" + i);
		}
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				cplex.addLe(x[i][j], cplex.prod(data.M[i][j], y[i][j]));
			}
		}
		cplex.exportModel("standard.lp");
	}

	// 求解MIP并获得结果
	public Solution solve() throws IloException {
		Solution s = new Solution();
		if (cplex.solve()) {
			s.cost = cplex.getObjValue();
			s.ship = new double[x.length][];
			s.link_y = new double[y.length][];
			for (int i = 0; i < x.length; i++) {
				s.ship[i] = cplex.getValues(x[i]);
				s.link_y[i] = cplex.getValues(y[i]);
			}
		}
		s.status = cplex.getCplexStatus();
		return s;
	}
}
