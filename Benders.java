package benders;

import java.util.Arrays;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class Benders {
	Data data;
	protected IloCplex subProblem;
	protected IloCplex master;
	IloObjective subobj;//记录子问题目标函数
	IloLinearNumExpr subobj_expr;
	//对偶变量
	protected IloNumVar[] u;	// 资源约束的对偶变量
	protected IloNumVar[] v;	// 需求约束的对偶变量
	protected IloNumVar[][] w;	// x,y约束的对偶变量
	double[] uSource;	//子问题中目标函数里对偶变量u对应系数
	double[] vDemand;	//子问题中目标函数里对偶变量v对应系数
	double[][] wM;		//子问题中目标函数里对偶变量w对应系数

	protected IloRange[][] subCon;	//子问题的约束方程
	double[][] xnum;				//记录原问题的x值

	protected IloNumVar subcost;	//子问题中的目标值，对应松弛的主问题模型中q
	protected IloNumVar[][] y;		//主问题中的变量
	public static final double FUZZ = 1.0e-7;

	int[][] y1;//子问题中变量y初始值
	public Benders(Data d) {
		// TODO Auto-generated constructor stub
		this.data = d;
	}
	//置1
	void setOne(int[][] a) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				a[i][j] = 1;
			}
		}
	}
	//建立主问题和子问题cplex模型
	public void bendersModel() throws IloException {
		subProblem = new IloCplex();//子问题
		master = new IloCplex();	//主问题
		subProblem.setOut(null);
		master.setOut(null);
		//参数初始化
		y1 = new int[data.SourcesSize][data.DemandsSize];
		setOne(y1);// 初始化参数y=[1]
		u = new IloNumVar[data.SourcesSize];
		v = new IloNumVar[data.DemandsSize];
		w = new IloNumVar[data.SourcesSize][data.DemandsSize];
		uSource = new double[data.SourcesSize];
		vDemand = new double[data.DemandsSize];
		wM = new double[data.SourcesSize][data.DemandsSize];
		y = new IloNumVar[data.SourcesSize][data.DemandsSize];
		subCon = new IloRange[data.SourcesSize][data.DemandsSize];
		xnum = new double[data.SourcesSize][data.DemandsSize];
		//参数约束
		subcost = master.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "subcost");
		for (int i = 0; i < data.SourcesSize; i++) {
			u[i] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "u_" + i);
		}
		for (int i = 0; i < data.DemandsSize; i++) {
			v[i] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "v_" + i);
		}
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				y[i][j] = master.numVar(0, 1, IloNumVarType.Int, "y_" + i + "_" + j);
				w[i][j] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "w_" + i + "_" + j);
			}
		}

		// 主问题
		IloNumExpr expr0 = master.numExpr();
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				expr0 = master.sum(expr0, master.prod(data.fixed_c[i][j], y[i][j]));
			}
		}
		master.addMinimize(master.sum(subcost, expr0), "TotalCost");
		// attach a Benders callback to the master
		master.use(new BendersCallback());

		//子问题
		// 子问题目标函数
		subobj_expr = subProblem.linearNumExpr();
		IloLinearNumExpr obj = subProblem.linearNumExpr();
		for (int i = 0; i < data.SourcesSize; i++) {
			uSource[i] = -data.supply[i];
			obj.addTerm(uSource[i], u[i]);
			subobj_expr.addTerm(uSource[i], u[i]);
		}
		for (int i = 0; i < data.DemandsSize; i++) {
			vDemand[i] = data.demand[i];
			obj.addTerm(vDemand[i], v[i]);
			subobj_expr.addTerm(vDemand[i], v[i]);
		}
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				wM[i][j] = -data.M[i][j];
				obj.addTerm(wM[i][j] * y1[i][j], w[i][j]);
			}
		}
		subobj = subProblem.addMaximize(obj, "dualSubCost");
		// 子问题约束方程
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				IloNumExpr expr = subProblem.numExpr();
				IloNumExpr expr1 = subProblem.numExpr();
				expr = subProblem.sum(subProblem.prod(-1, u[i]), v[j]);
				expr1 = subProblem.sum(expr, subProblem.prod(-1, w[i][j]));
				subCon[i][j] = subProblem.addLe(expr1, data.c[i][j],"C"+i+"_"+j);
			}
		}
		// turn off the presolver for the main model
		subProblem.setParam(IloCplex.BooleanParam.PreInd, false);
		subProblem.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
	}
	//benders求解过程中添加约束
	private class BendersCallback extends IloCplex.LazyConstraintCallback {
		public void main() throws IloException {
			double zMaster = getValue(subcost);//从主问题中获得subcost参数的值
			//获得主问题中的变量y值
			int[][] y2 = new int[data.SourcesSize][data.DemandsSize];
			for (int i = 0; i < data.SourcesSize; i++) {
				for (int j = 0; j < data.DemandsSize; j++) {
					double aa = getValue(y[i][j]);
					if (aa > 0.5) {
						y2[i][j] = 1;
					} else {
						y2[i][j] = 0;
					}
				}
			}
			//根据松弛的主问题中的 变量y值重置子问题目标函数
//			subProblem.remove(subobj);
			 IloLinearNumExpr subobj_expr0 = subProblem.linearNumExpr();
			 for (int i = 0; i < data.SourcesSize; i++) {
					subobj_expr0.addTerm(uSource[i], u[i]);
				}
				for (int i = 0; i < data.DemandsSize; i++) {
					subobj_expr0.addTerm(vDemand[i], v[i]);
				}
				for (int i = 0; i < data.SourcesSize; i++) {
					for (int j = 0; j < data.DemandsSize; j++) {
						subobj_expr0.addTerm(wM[i][j] * y2[i][j], w[i][j]);
					}
				}
//			subobj_expr1 = (IloLinearNumExpr) subProblem.sum(subobj_expr,subobj_expr1);
//			subobj = subProblem.addMaximize(subobj_expr0, "dualSubCost");
			subobj.setExpr(subobj_expr0);//重置子问题目标函数
			subProblem.solve();
			IloCplex.Status status = subProblem.getStatus();
			//判断子问题的求解状态
			if (status == IloCplex.Status.Unbounded) {
				// 获得射线
				IloLinearNumExpr ray = subProblem.getRay();//获得子问题的一条极射线
				System.out.println("getRay returned " + ray.toString());
				//记录极射线的参数
				IloLinearNumExprIterator it = ray.linearIterator();
				double[] ucoef = new double[data.SourcesSize];	//极射线中u的系数
				double[] vcoef = new double[data.DemandsSize];	//极射线中v的系数
				double[][] wcoef = new double[data.SourcesSize][data.DemandsSize];//极射线中w的系数
				while (it.hasNext()) {
					IloNumVar var = it.nextNumVar();
					boolean varFound = false;
					for (int i = 0; i < data.SourcesSize && !varFound; i++) {
						if (var.equals(u[i])) {
							ucoef[i] = it.getValue();
							varFound = true;
						}
						for (int j = 0; j < data.DemandsSize && !varFound; j++) {
							if (var.equals(w[i][j])) {
								wcoef[i][j] = it.getValue() * wM[i][j];
								varFound = true;
							}
						}
					}
					for (int i = 0; i < data.DemandsSize && !varFound; i++) {
						if (var.equals(v[i])) {
							vcoef[i] = it.getValue();
							varFound = true;
						}
					}
				}
				//构造要添加到约束方程
				IloNumExpr expr1 = master.numExpr();
				double expr2 = 0;
				for (int i = 0; i < data.SourcesSize; i++) {
					expr2 += ucoef[i] * uSource[i];
					expr1 = master.sum(expr1, master.scalProd(wcoef[i], y[i]));
				}
				for (int j = 0; j < data.DemandsSize; j++) {
					expr2 += vcoef[j] * vDemand[j];
				}
				//添加约束方程到主问题中
				IloConstraint r = add(master.le(master.sum(expr1, expr2), 0));
				System.out.println("\n>>> Adding feasibility cut: " + r + "\n");

			} else if (status == IloCplex.Status.Optimal) {
				if (zMaster < subProblem.getObjValue() - FUZZ) {
					//子问题有解，则最优解即一个极值点
					double[] ucoef = new double[data.SourcesSize];//极点中u的系数
					double[] vcoef = new double[data.DemandsSize];//极点中v的系数
					double[][] wcoef = new double[data.SourcesSize][data.DemandsSize];//极点中w的系数
					ucoef = subProblem.getValues(u);
					vcoef = subProblem.getValues(v);
					for (int i = 0; i < data.SourcesSize; i++) {
						wcoef[i] = subProblem.getValues(w[i]);
					}
					
					//构造要添加到约束方程
					double expr3 = 0;
					IloNumExpr expr4 = master.numExpr();
					for (int i = 0; i < data.SourcesSize; i++) {
						expr3 += ucoef[i] * uSource[i];
						for (int j = 0; j < data.DemandsSize; j++) {
							wcoef[i][j] = wcoef[i][j] * wM[i][j];
						}
					}
					for (int j = 0; j < data.DemandsSize; j++) {
						expr3 += vcoef[j] * vDemand[j];
					}
					for (int i = 0; i < data.SourcesSize; i++) {
						expr4 = master.sum(expr4, master.scalProd(wcoef[i], y[i]));
					}
					//添加约束方程到主问题中
					IloConstraint r = add((IloRange) master.le(master.sum(expr3, expr4), subcost));
					System.out.println("\n>>> Adding optimality cut: " + r + "\n");
				} else {
					System.out.println("\n>>> Accepting new incumbent with value " + getObjValue() + "\n");
					// the master and subproblem flow costs match
					// -- record the subproblem flows in case this proves to be the
					// winner (saving us from having to solve the LP one more time
					// once the master terminates)
					//主问题中subcost值和子问题中的最优解值相等
					for (int i = 0; i < data.SourcesSize; i++) {
						xnum[i] = subProblem.getDuals(subCon[i]);
					}
				}
			} else {
				// unexpected status -- report but do nothing
				//出现不希望出现的状态，非法
				System.err.println("\n!!! Unexpected subproblem solution status: " + status + "\n");
			}
		}
	}
	//求解benders模型并复制结果
	public final Solution solve() throws IloException {
		Solution s = new Solution();
		//记录结果
		if (master.solve()) {
			s.cost = master.getObjValue();
			s.ship = new double[data.SourcesSize][];
			s.link_y = new double[data.SourcesSize][];
			for (int i = 0; i < data.SourcesSize; i++) {
				s.link_y[i] = master.getValues(y[i]);
				s.ship[i] = Arrays.copyOf(xnum[i], data.DemandsSize);
			}
		}
		s.status = master.getCplexStatus();
		return s;
	}

}
