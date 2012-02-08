package playground.kai.bvwp;


public class Main_BVWP2003 {

	public static void main(String[] args) {
		System.out.println("\n===\nFolgende Rechnung entspricht exakt ``rechnungen>javaRechnungen>BVWP2003.xlsx'' (EconomicValues, Scenario und Methodik) ");
		
		// create the economic values
		Values economicValues = EconomicValues_ExcelPV.createEconomicValues1();

		// create the base case:
		ScenarioForEval nullfall = Scenario_ExcelPV.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Scenario_ExcelPV.createPlanfall1(nullfall);
		
		// instantiate the class that defines the utility calculation:
		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();

		// compute the utilities (currently, results are printed to the console):
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
		
	}

}
