package benders;

import ilog.concert.IloException;

public class bendersTest {
/*
 * 此代码分三部分：1.直接建立cplex模型求解。2.建立主问题和子问题的cplex模型，调用cplex内部Benders求解函数求解。
			  3.建立主问题和子问题的cplex模型，采用推文中伪代码Benders求解过程求解。
                              这三部分分别对应SingleModel类、Benders类、Benders_next类。
 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Data data = new Data();
		String path = "data/test1.txt";
		data.read_data(path);
	    long start;
	    long end;
	    //建立简单的标准模型求解(SingleModel)
	    try{
	    	start = System.currentTimeMillis();
	        System.out.println("\n================================="
                    + "\n== Solving the usual MIP model =="
                    + "\n=================================");
	        SingleModel model = new SingleModel();
	        model.standardMIP(data);
	        Solution s = model.solve();
	        end = System.currentTimeMillis();
	        System.out.println("\n***\nThe unified model's solution has total cost "
                    + String.format("%10.5f", s.cost));
	        System.out.println();
	        s.print_ship();
	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
	        
	    } catch (IloException ex) {
	        System.err.println("\n!!!Unable to solve the unified model"
                    + ex.getMessage() + "\n!!!");
	        System.exit(1);
	    }
	    
	    //benders算法求解模型(Benders)
	    try{
	    	start = System.currentTimeMillis();
	        System.out.println("\n======================================="
                    + "\n== Solving via Benders decomposition =="
                    + "\n=======================================");
	        Benders model2 = new Benders(data);
	        model2.bendersModel();
	        Solution s = model2.solve();
	        end = System.currentTimeMillis();
	        System.out.println("\n***\nThe benders model's solution has total cost "
                    + String.format("%10.5f", s.cost));
	        System.out.println();
	        s.print_ship();
	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
	    }catch (IloException ex) {
	        System.err.println("\n!!!Unable to solve the Benders model:\n"
                    + ex.getMessage() + "\n!!!");
	        System.exit(2);
	    }
	    //推文中实现方法(Benders_next)
	    try{
	    	start = System.currentTimeMillis();
	        System.out.println("\n======================================="
                    + "\n== Solving via Benders decomposition =="
                    + "\n=======================================");
	        Benders_next model3 = new Benders_next(data);
	        model3.bendersModel();
	        Solution s = model3.solve();
	        end = System.currentTimeMillis();
	        System.out.println("\n***\nThe benders model's solution has total cost "
                    + String.format("%10.5f", s.cost));
	        System.out.println();
	        s.print_ship();
	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
	    }catch (IloException ex) {
	        System.err.println("\n!!!Unable to solve the Benders model:\n"
                    + ex.getMessage() + "\n!!!");
	        System.exit(2);
	    }
	}

}
