
import java.util.Random;

/**
 * This class is just a dummy service called by the Workflow each time a task is
 * to be executed. It computes the sum of the first N natural numbers, where N is given in the SILK file
 * 
 * @author Marc Frincu v0.2 June 5th 2009
 * @since 2008
 * 
 */
public class DummyServiceExample {
	public Object doSomething(String data, String taskToBeSolved)
			throws Exception {
		Random rand = new Random();
		Thread.sleep(rand.nextInt(1000));

		String[] inf = data.split("#");
		Integer sum = 0;

		// If the command is to add the numbers
		if (taskToBeSolved.compareTo("add") == 0) {
			
			if (inf.length != 2)
				return null;
			
			for (int i = 0; i < inf.length; i++) {
				if (inf[i].trim().compareTo("") != 0)
					sum += Integer.parseInt(inf[i]);
			}

			return sum;
		}

		// If the command is to return the succesor of the number
		if (taskToBeSolved.compareTo("return-successor") == 0) {
			if (inf.length == 1)
				return Integer.parseInt(inf[0])+1;
			else
				return null;
		}

		return null;

	}
}
