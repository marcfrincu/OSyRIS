package osyris.ruleconvertor;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import osyris.util.TaskInfo;

/**
 * This class contains the basic functionality to convert a <i>SImple Language
 * for worKflows</i> (SILK) file to a <i>Drools</i> (DRL) rule file
 * 
 * @author Marc Frincu, v0.1, Feb 13th 2009
 * @author Marc Frincu, v0.2, May 27th 2009
 * @since 2008
 * 
 */
public class SILK2Drools {

	private String filename = null, drlfile = null;
	private StringBuilder fileContent = null, output = null;
	private Hashtable<String, TaskInfo> tasks = null;
	private Hashtable<String, String> lhsOutputBindings = null,
			rhsInputBindings = null;
	private Hashtable<Integer, Integer> ruleDomains = null;
	private Hashtable<String, TaskBehaviour> tasksLHSBehaviour = null,
			tasksRHSBehaviour = null;
	private Hashtable<String, Hashtable<String, String>> taskLHSOutputBindings = null,
			taskRHSInputBindings = null;
	private int taskIndex = 0;

	/**
	 * Constructor
	 * 
	 * @param in
	 *            the <i>SILK</i> input file containing the absolute path to it
	 * @param out
	 *            the <i>DRL</i> output file containing the absolute path to it
	 *            {@code}
	 */
	public SILK2Drools(String in, String out) {
		this.filename = in;
		this.drlfile = out;
	}

	/**
	 * This method is responsible for translating the <i>SILK</i> file into a
	 * <i>DRL</i> file. The syntax for a <i>SILK</i> file is as follows:
	 * @param packageName the name of the package containing the rules
	 * @throws Exception
	 */
	public int makeRules(String packageName) throws Exception {
		ArrayList<String> cmds = getCmdsFromFile(this.filename);
		ArrayList<String> rhsTasks = null, lhsTasks = null;
		this.tasks = new Hashtable<String, TaskInfo>();
		this.lhsOutputBindings = new Hashtable<String, String>();
		this.ruleDomains = new Hashtable<Integer, Integer>();
		this.rhsInputBindings = new Hashtable<String, String>();
		this.taskLHSOutputBindings = new Hashtable<String, Hashtable<String, String>>();
		this.taskRHSInputBindings = new Hashtable<String, Hashtable<String, String>>();
		this.tasksRHSBehaviour = new Hashtable<String, TaskBehaviour>();
		this.tasksLHSBehaviour = new Hashtable<String, TaskBehaviour>();
		String lhs = "", rhs = "", condition = "";
		this.output = new StringBuilder();
		int ruleIndex = 0;
		Integer ruleDomain = 0;

		StringBuilder header = new StringBuilder("package " + packageName + "\n");
		header.append("\n");
		header
				.append("# This file was automatically generated using the SILK2Drools class\n");
		header.append("# part of the OSyRIS Workflow Engine developed at\n");
		header
				.append("# the West University of Timisoara, Faculty of Mathematics and Computer Science Romania\n");
		header
				.append("# For questions and/or remarks mail to: mfrincu@info.uvt.ro\n\n");
		header.append("import osyris.workflow.State;\n");
		header.append("import osyris.workflow.WFResource;\n");
		header.append("import org.apache.log4j.Logger;\n");
		header.append("import osyris.workflow.Runner;\n");
		header.append("\n");
		header.append("global org.apache.log4j.Logger log;\n");
		header.append("global java.util.ArrayList runner;\n");

		output.append(header);

		try {

			// for each command we create a rule
			for (String c : cmds) {
				// clear the Hashtable. start fresh for each rule
				// this.lhsOutputBindings.clear();
				// this.rhsInputBindings.clear();
				this.taskLHSOutputBindings.clear();
				this.taskRHSInputBindings.clear();
				this.tasksRHSBehaviour.clear();
				this.tasksLHSBehaviour.clear();
				if (c.indexOf("->") != -1) {
					//System.out.println(c);
					// we have a rule: LHS -> RHS
					lhs = c.split("->")[0];
					rhs = c.split("->")[1];
					// check to see if the rule belongs to a domain
					if (lhs.indexOf(":") > 0) {
						try {
							ruleDomain = new Integer(lhs.substring(0,
									lhs.indexOf(":")).trim());
							this.ruleDomains.put(ruleDomain, ruleDomain);
						} catch (NumberFormatException nfe) {
							throw new Exception("Invalid group name '"
									+ lhs.substring(0, lhs.indexOf(":"))
									+ "'. Integer value expected.");
						}
						lhs = lhs.substring(lhs.indexOf(":") + 1);
					}
					if (lhs.indexOf(":") == 0)
						throw new Exception(
								"Invalid character ':'. ':' should come after an integer domain ID.");

					// check to see if the have a conditions,
					// i.e. LHS -> RHS | cond
					// cond may also include a priority in the range from 0 to
					// 10
					condition = "";
					if (rhs.indexOf("|") != -1) {
						condition = rhs.substring(rhs.indexOf("|") + 1, rhs
								.length());
						rhs = rhs.substring(0, rhs.indexOf("|"));
					}

					lhsTasks = this.getTasks(c, lhs, true);
					rhsTasks = this.getTasks(c, rhs, false);

					this.addTasks(lhsTasks);
					this.addTasks(rhsTasks);

					/*
					 * System.out.println("Rule " + lhs + " -> " + rhs);
					 * System.out.println("LHS: " + lhsTasks);
					 * System.out.println("Cond: " + condition);
					 * System.out.println("RHS: " + rhsTasks);
					 */

					this.generateRule(c, lhsTasks, rhsTasks, condition,
							ruleIndex, ruleDomain);

					ruleIndex++;
				} else {
					// we have a task definition:
					// A=[state=0,input:"data",output:"data",task:"some
					// problem"]
					// every attribute (state, input, output, task) is optional
					// state can have one of the following values: 0 (NOT RUN),
					// 1 (RUNNING) and 2 (FINISHED)
					if (c.indexOf(":=") != -1) {
						final String taskName = c.split(":=")[0].trim();
						final String taskDefTmp = c.split(":=")[1].trim();
						final String taskDef = taskDefTmp.substring(taskDefTmp
								.indexOf("[") + 1, taskDefTmp.indexOf("]"));

						final String[] taskAttrs = taskDef.split(",");

						final TaskInfo ti = new TaskInfo(taskIndex++, taskName);
						String[] taskAttrComponents = null;
						String taskAttrInitValue = null, attrType = null;
						String part1 = null, part2 = null;
						for (String ta : taskAttrs) {
							// we have a meta-attribute
							if (ta.indexOf("=") != -1) {
								part1 = ta.substring(0, ta.indexOf("=")).trim();
								part2 = ta.substring(ta.indexOf("=") + 1)
										.trim();

								if (part1.startsWith("\"")
										&& part1.endsWith("\"")
										&& part2.startsWith("\"")
										&& part2.endsWith("\"")) {
									ti.setMetaAttributes(part1.substring(1,
											part1.length() - 1).toLowerCase(),
											part2.substring(1,
													part2.length() - 1));
									continue;
								}
							}

							// else we have a normal attribute
							taskAttrComponents = ta.split(":");
							int lastIndex = ta.lastIndexOf(':');
							int indexOfEqual = ta.indexOf('=');
							// an attribute is defined as i1:input
							if ((taskAttrComponents.length == 2)
									|| (lastIndex > indexOfEqual && taskAttrComponents.length > 2)) {
								// it can also have an optional initialization
								// value
								// i1:input=initValue
								if (taskAttrComponents.length > 2)
									taskAttrComponents[1] = ta.substring(ta
											.indexOf(":") + 1);
								if (taskAttrComponents[1].indexOf("=") != -1) {
									attrType = taskAttrComponents[1].substring(
											0,
											taskAttrComponents[1].indexOf("="))
											.trim();
									taskAttrInitValue = taskAttrComponents[1]
											.substring(taskAttrComponents[1]
													.indexOf("=") + 1);
								} else {
									attrType = taskAttrComponents[1].trim();
								}
								if (attrType.toLowerCase().compareTo("input") == 0) {
									ti.addInput(taskAttrComponents[0].trim(),
											taskAttrInitValue);
								} else {
									if (attrType.toLowerCase().compareTo(
											"output") == 0) {
										ti.addOutput(taskAttrComponents[0]
												.trim(), taskAttrInitValue);
									} else {
										throw new Exception(
												"Unknown attribute type: "
														+ attrType
														+ " in task definition: "
														+ c);
									}
								}

							} else {
								throw new Exception(
										"Error defining the task attributes for task: "
												+ c
												+ ". Expected var:type=value format");
							}
						}
						tasks.put(taskName, ti);
					} else {
						throw new Exception("Unknown construct: " + c);
					}
				}
				this.writeDRLFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error parsing the SILK file:\n"
					+ e.getMessage());
		}
		return ruleIndex;
	}

	/**
	 * Adds new tasks to the task list
	 * 
	 * @param newTasks
	 */
	private void addTasks(ArrayList<String> newTasks) {
		boolean exists = false;
		for (String t : newTasks) {
			Enumeration<String> en = this.tasks.keys();
			String key = null;
			exists = false;
			while (en.hasMoreElements()) {
				key = en.nextElement();
				// eliminate the optional ruleDomain if present
				t = t.substring(t.indexOf(":") + 1).trim();
				if (key.compareTo(t) == 0) {
					exists = true;
				}
			}

			if (exists == false) {
				this.tasks.put(t, new TaskInfo(taskIndex++, t));
			}
		}
	}

	/**
	 * This method is used to generate a rule based on the <i>RHS</i>,
	 * <i>LHS</i> and an optional <i>Condition</i>
	 * 
	 * @param rhsTasks
	 *            the list of tasks in the <i>RHS</i> of the rule
	 * @param lhsTasks
	 *            the list of tasks in the <i>LHS</i> of the rule
	 * @param condition
	 *            the optional <i>Condition</i>
	 * @param ruleIndex
	 *            the index of the rule. It is automatically increased with each
	 *            new rule
	 * @param ruleDomain
	 *            the domain this rules belongs to. It must be an integer value.
	 *            By default if a rule has no domain than it is considered to be
	 *            MAIN
	 * @throws Exception
	 */
	private void generateRule(String ruleLine, ArrayList<String> lhsTasks,
			ArrayList<String> rhsTasks, String condition, int ruleIndex,
			int ruleDomain) throws Exception {

		Hashtable<String, String> tmpProcessedNodes = new Hashtable<String, String>();
		TaskInfo ti = null;
		TaskBehaviour tb = null;
		Integer linkedToDomain = null;
		int noLinkedDomains = 0;

		StringBuilder rule = new StringBuilder("\n");
		rule.append("rule \"" + (ruleIndex) + "\"\n");
		rule.append("\tno-loop false\n");
		if (new Integer(ruleDomain).toString().compareTo("0") != 0)
			rule.append("\tagenda-group \"" + ruleDomain + "\"\n");
		StringBuilder whenClause = new StringBuilder("\twhen\n");
		// whenClause
		// .append("\t\t$resource : WFResource( $runningThreads :
		// runningThreads, $url : url )\n");
		// whenClause
		// .append("\t\tnot WFResource( runningThreads < $runningThreads )\n");
		StringBuilder thenClause = new StringBuilder(
				"\tthen\n\t\tlog.debug(\"Firing rule: \"+" + (ruleIndex)
						+ ");\n\t\t((Runner)runner.get(" + ruleIndex
						+ ")).clearAllStates();\n");
		// create WHEN and THEN parts of the rule
		String state = null, condNew = null;
		Hashtable<String, String> conditionTasks = new Hashtable<String, String>();
		Hashtable<String, String> inputBindings = null, outputBindings = null;
		if (condition.length() > 0) {
			// Check for Salience. can be positive or negative. Default is 0
			if (condition.indexOf(",") != -1) {
				String salience = condition
						.substring(condition.indexOf(",") + 1);
				try {
					Integer.parseInt(salience);
					rule.append("\tsalience " + salience + "\n");
				} catch (NumberFormatException nfe) {
					throw new Exception(
							"Error in rule: "
									+ ruleLine
									+ ". Salience is expected to be an Integer value.  Found: "
									+ salience);
				}
				condition = condition.substring(0, condition.indexOf(","));
			}

			String[] conditions = condition.toLowerCase().split("and|or"), operators = null;
			ArrayList<String> operand = null;
			for (String cond : conditions) {
				condNew = cond;
				operators = cond.split("[><!=]+");
				operand = this.parse(cond, "([><!=]+)", 1);
				if (operand.size() != 1) {
					throw new Exception("Error in rule: " + ruleLine
							+ ". Invalid operand: " + operand);
				}
				if (operators.length != 2)
					throw new Exception("Invalid condition: " + cond);
				/*
				 * for each side of the condition: operand1 operator operand2
				 * check whether operand[1,2] has variables binded to a task in
				 * the LHS. if so add it to the rule
				 */
				String keyVar = operators[0].trim();
				String keyTask = null, keyVar2 = null;
				Enumeration<String> en = this.taskLHSOutputBindings.keys();
				// take all tasks
				boolean found = false;
				while (en.hasMoreElements()) {
					keyTask = en.nextElement();
					// get their bindings
					outputBindings = this.taskLHSOutputBindings.get(keyTask);
					Enumeration<String> en2 = outputBindings.keys();
					while (en2.hasMoreElements()) {
						keyVar2 = en2.nextElement();
						// if the binding key is equal to my operator then it is
						// binded to the task
						if (keyVar2.compareTo(keyVar) == 0) {
							state = "\t\t$state"
									+ this.tasks.get(keyTask).index
									+ " : State ( index == "
									// + this.tasks.get(keyTask).index
									// + ", state == State.FINISHED )\n";
									+ this.tasks.get(keyTask).index + " )\n"
									+ "\t\teval ( $state"
									+ this.tasks.get(keyTask).index
									+ ".getNoInstances() > 0 )\n";
							found = true;
							if (tmpProcessedNodes.get(state) == null) {
								whenClause.append(state);

								// Decrement no of instances if we need to
								tb = this.tasksLHSBehaviour.get(keyTask);
								if (tb != null) {
									if (tb.getBehaviourName().toLowerCase()
											.compareTo("consume") == 0
											&& tb.getBehaviourValue()
													.toLowerCase().compareTo(
															"false") != 0)
										thenClause
												.append("\t\tmodify( $state"
														+ this.tasks
																.get(keyTask).index
														+ " )  { decrementNoInstances() };\n");
								} else
									thenClause
											.append("\t\tmodify( $state"
													+ this.tasks.get(keyTask).index
													+ " )  { decrementNoInstances() };\n");

								tmpProcessedNodes.put(state, state);
							}
							condNew = "$state" + this.tasks.get(keyTask).index
									+ ".getOutput(\""
									+ outputBindings.get(keyVar2) + "\")";

						}
					}
				}
				if (found == false) {
					throw new Exception(
							"Error in rule: "
									+ ruleLine
									+ ". Left operand must have a binding variable. Variable '"
									+ keyVar + "' not bound");
				}

				keyVar = operators[1].trim().toLowerCase();
				en = this.taskLHSOutputBindings.keys();
				boolean found2 = false;
				while (en.hasMoreElements()) {
					keyTask = en.nextElement();
					// get their bindings
					outputBindings = this.taskLHSOutputBindings.get(keyTask);
					Enumeration<String> en2 = outputBindings.keys();
					while (en2.hasMoreElements()) {
						keyVar2 = en2.nextElement();
						// if the binding key is equal to my operator then it is
						// binded to the task
						if (keyVar2.compareTo(keyVar) == 0) {
							state = "\t\t$state"
									+ this.tasks.get(keyTask).index
									+ " : State ( index == "
									+ this.tasks.get(keyTask).index
									// + ", state == State.FINISHED )\n";
									+ " )\n" + "\t\teval ( $state"
									+ this.tasks.get(keyTask).index
									+ ".getNoInstances() > 0 )\n";

							found2 = true;
							if (tmpProcessedNodes.get(state) == null) {
								whenClause.append(state);

								// Decrement no of instances if we need to
								tb = this.tasksLHSBehaviour.get(keyTask);
								if (tb != null) {
									if (tb.getBehaviourName().toLowerCase()
											.compareTo("consume") == 0
											&& tb.getBehaviourValue()
													.toLowerCase().compareTo(
															"false") != 0)
										thenClause
												.append("\t\tmodify( $state"
														+ this.tasks
																.get(keyTask).index
														+ " )  { decrementNoInstances() };\n");
								} else
									thenClause
											.append("\t\tmodify( $state"
													+ this.tasks.get(keyTask).index
													+ " )  { decrementNoInstances() };\n");

								tmpProcessedNodes.put(state, state);
							}
							condNew = condNew + ".toString().compareTo($state"
									+ this.tasks.get(keyTask).index
									+ ".getOutput(\""
									+ outputBindings.get(keyVar2)
									+ "\").toString()) " + operand.get(0)
									+ " 0 ";
						}
					}
				}
				if (found2 == false) {
					if (keyVar.startsWith("\"") && keyVar.endsWith("\"")) {
						condNew = condNew + ".toString().compareTo(" + keyVar
								+ ") " + operand.get(0) + " 0 ";
					} else {
						try {
							Double.parseDouble(keyVar);
							condNew = "Double.parseDouble( " + condNew
									+ ".toString() ) " + operand.get(0) + " "
									+ keyVar;
						} catch (NumberFormatException nfe) {
							throw new Exception(
									"Error in rule: "
											+ ruleLine
											+ ". Operand "
											+ keyVar
											+ " is expected to be either an Integer/Double value or a String.  Found: "
											+ keyVar);
						}
					}
				}

				if (found == true) {
					condNew = "\t\teval ( " + condNew + " )\n";
				}

				if (found == false && found2 == true) {
					throw new Exception(
							"Binding variable must be on the left side of the condition. Found: "
									+ condition);
				}

				condition = condition.replace(cond, condNew);
			}
			whenClause.append(condition);
		}
		for (String l : lhsTasks) {
			// System.out.println(this.tasks);
			// System.out.println(l);
			ti = this.tasks.get(l.trim());
			if (ti != null) {
				state = "\t\t$state" + ti.index + " : State ( index == "
						+ ti.index
						// + ", state == State.FINISHED )\n";
						+ " )\n" + "\t\teval ( $state" + ti.index
						+ ".getNoInstances() > 0 )\n";
				if (tmpProcessedNodes.get(state) == null) {
					whenClause.append(state);

					// Decrement no of instances if we need to
					tb = this.tasksLHSBehaviour.get(ti.name);
					if (tb != null) {
						if (tb.getBehaviourName().toLowerCase().compareTo(
								"consume") == 0
								&& tb.getBehaviourValue().toLowerCase()
										.compareTo("false") != 0)
							thenClause.append("\t\tmodify( $state"
									+ this.tasks.get(ti.name).index
									+ " )  { decrementNoInstances() };\n");
					} else
						thenClause.append("\t\tmodify( $state"
								+ this.tasks.get(ti.name).index
								+ " )  { decrementNoInstances() };\n");

					tmpProcessedNodes.put(state, state);
				}
				// thenClause.append("\t\tinputItems.add( $state" + ti.index
				// + ".getOutput() );\n");
			}
		}

		for (String r : rhsTasks) {
			if (r.indexOf(":") > 0) {
				try {
					linkedToDomain = new Integer(r.substring(0, r.indexOf(":"))
							.trim());
					noLinkedDomains++;
					if (noLinkedDomains > 1) {
						throw new Exception(
								"Cannot link to more than one domain ID. The linkage must be accomplished a single time. Rule: "
										+ ruleLine);
					}
					/*
					 * if (this.ruleDomains.get(linkedToDomain) == null) throw
					 * new Exception("Error in rule " + ruleLine + ". Cannot
					 * find domain '" + linkedToDomain + "' which task '" + r +
					 * "' points to");
					 */
				} catch (NumberFormatException nfe) {
					throw new Exception(
							"Expecting an Integer value for domain ID in RHS task "
									+ r + " part of rule " + ruleLine);
				}
			}
			if (r.indexOf(":") == 0)
				throw new Exception("Invalid ':' in RHS task " + r
						+ " part of rule " + ruleLine
						+ ". ':' Should follow the domain ID (Integer value)");

			r = r.substring(r.indexOf(":") + 1).trim();
			ti = this.tasks.get(r);
			if (ti != null) {

				inputBindings = this.taskRHSInputBindings.get(r);
				if (inputBindings == null) {
					throw new Exception("RHS task without input bindings found: " + r);
				}
				Enumeration<String> en = inputBindings.keys();
				Enumeration<String> en2 = null;
				boolean found = false;
				// for every binding variable in the RHS
				while (en.hasMoreElements()) {
					String key = en.nextElement();
					en2 = this.taskLHSOutputBindings.keys();
					// take all tasks in the LHS
					found = false;
					while (en2.hasMoreElements() && found == false) {
						String key2 = en2.nextElement();
						outputBindings = this.taskLHSOutputBindings.get(key2);
						// and foreach task check if it has a binding variable
						// equal to the variable in the RHS
						if (outputBindings.get(key) != null) {
							thenClause.append("\t\t$state" + ti.index
									+ ".setInput(\"" + inputBindings.get(key)
									+ "\", $state" + this.tasks.get(key2).index
									+ ".getOutput(\"" + outputBindings.get(key)
									+ "\"), " + this.tasks.get(key2).index
									+ " );\n");

							tb = this.tasksRHSBehaviour.get(ti.name);
							if (tb != null) {
								if (tb.getBehaviourName().toLowerCase()
										.compareTo("instances") == 0)
									thenClause.append("\t\t$state"
											+ this.tasks.get(ti.name).index
											+ ".setNoInstancesToBeCreated( "
											+ tb.getBehaviourValue() + " );\n");
							} else
								thenClause.append("\t\t$state"
										+ this.tasks.get(ti.name).index
										+ ".setNoInstancesToBeCreated( 1 );\n");
							found = true;
						}
					}
					if (found == false) {
						throw new Exception("Error in rule: " + ruleLine
								+ ". Binding variable: " + key
								+ " found in RHS but not in LHS");
					}
				}

				state = "\t\t$state" + ti.index + " : State ( index == "
						+ ti.index
						// + ", state == State.NOTRUN )\n";
						+ " )\n";
				// If the rule is recursive (l appears in LHS) do not add it. It
				// will be added when the RHS side will be processed
				// Also if l appears in the Condition do not add it
				if (tmpProcessedNodes.get(state) == null
						&& this.isInLHS(lhsTasks, r) == false
						&& conditionTasks.get(r) == null) {
					whenClause.append(state);
					tmpProcessedNodes.put(state, state);
				}
				// thenClause.append("\t\t$state" + ti.index
				// + ".setUrl( $url );\n");
				thenClause.append("\t\t((Runner)runner.get(" + ruleIndex
						+ ")).addState( $state" + ti.index + " );\n");
			}
		}
		//if (linkedToDomain != null)
			// thenClause
			// .append("\t\tkcontext.getKnowledgeRuntime().getAgenda().getAgendaGroup( \""
			// + linkedToDomain + "\" ).setFocus();\n");

			output.append(rule.toString());
		output.append(whenClause.toString());
		output.append(thenClause.toString());
		output.append("\t\t((Runner)runner.get(" + ruleIndex
				+ ")).executeAll();\n");
		output.append("\t\tlog.debug(\"Sent tasks for execution\");\n");
		if (linkedToDomain != null)
		output
				.append("\t\tkcontext.getKnowledgeRuntime().getAgenda().getAgendaGroup( \""
						+ linkedToDomain + "\" ).setFocus();\n");
		output.append("end\n");
	}

	/**
	 * Reads a command file and returns the list of commands. The commands are
	 * returned in the same order as found in the file
	 * 
	 * @param filename
	 * @return the list of commands in the order found in the given file
	 * @throws IOException
	 */
	private ArrayList<String> getCmdsFromFile(String filename)
			throws IOException {
		ArrayList<String> commands = new ArrayList<String>();
		BufferedReader input = new BufferedReader(new FileReader(filename));
		fileContent = new StringBuilder();

		try {
			String line = null;
			String[] elems = null;
			/*
			 * readLine returns the content of a line MINUS the newline. it
			 * returns null only for the END of the stream. it returns an empty
			 * String if two newlines appear in a row.
			 */
			while ((line = input.readLine()) != null) {
				fileContent.append(line);
				fileContent.append(System.getProperty("line.separator"));

				// skip comments and empty lines
				if (line.trim().startsWith("#") || line.trim().length() == 0)
					continue;

				elems = line.split(";");
				for (String e : elems) {
					if (e.trim().length() != 0)
						commands.add(e.trim());
				}
			}

		} finally {
			input.close();
		}
		return commands;
	}

	/**
	 * Writes the <i>DRL</i> file
	 * 
	 * @throws Exception
	 */
	private void writeDRLFile() throws Exception {
		FileOutputStream drl = new FileOutputStream(this.drlfile);
		PrintStream fdrl = new PrintStream(drl);

		fdrl.println(this.output.toString());
		fdrl.close();
	}

	/**
	 * Parses a String given a RegExp pattern and returns an ArrayList with the
	 * matched items
	 * 
	 * @param line
	 * @param patternStr
	 * @return an ArrayList with matched patterns
	 */
	private ArrayList<String> parse(String line, String patternStr,
			int groupIndex) {
		ArrayList<String> requiredIds = new ArrayList<String>();

		Pattern pattern = Pattern.compile(patternStr);

		// Check for the existence of the pattern
		Matcher matcher = pattern.matcher(line);

		boolean matchFound = matcher.find();
		while (matchFound) {
			// Retrieve matching string
			requiredIds.add(matcher.group(groupIndex));
			// Find the next occurrence
			matchFound = matcher.find();
		}
		return requiredIds;
	}

	private ArrayList<String> getTasks(String ruleLine, String list, boolean lhs)
			throws Exception {
		ArrayList<String> tasks = new ArrayList<String>();
		final String[] taskList = list.split(",");
		String task = null, taskComplete = null;
		String[] bindings = null, parts = null;
		for (String item : taskList) {
			this.rhsInputBindings = new Hashtable<String, String>();
			this.lhsOutputBindings = new Hashtable<String, String>();
			if (item.indexOf("[") != -1 && item.indexOf("]") != -1
					&& item.indexOf("[") < item.indexOf("]")) {
				task = item.substring(0, item.indexOf("[")).trim();
				taskComplete = task;
				if (task.indexOf(":") != -1)
					task = task.substring(task.indexOf(":") + 1).trim();
				tasks.add(taskComplete);
				// [A=o1#B=o2]
				bindings = item.substring(item.indexOf("[") + 1,
						item.indexOf("]")).trim().split("#");
				for (String b : bindings) {
					parts = b.split("=");
					if (parts.length == 2) {
						if (lhs == true) { // LHS
							if (this.isOutputDeclaredInTask(task, parts[1]) == true) {
								if (this.lhsOutputBindings.put(parts[0],
										parts[1]) != null) {
									throw new Exception(
											"Error in rule: "
													+ ruleLine
													+ ". Duplicate attribute identifier: "
													+ parts[0] + " for LHS: "
													+ list);
								}
							} else {
								// Handle the consume of LHS tasks
								if (parts[0].toLowerCase().compareTo("consume") == 0
										&& (parts[1].toLowerCase().compareTo(
												"false") == 0 || parts[1]
												.toLowerCase()
												.compareTo("true") == 0)) {
									this.tasksLHSBehaviour.put(task,
											new TaskBehaviour(parts[0],
													parts[1]));
								} else {
									throw new Exception("Error in rule: "
											+ ruleLine + ". Output attribute: "
											+ parts[1]
											+ " undeclared in Task: " + task);
								}
							}
						} else { // RHS
							if (this.isInputDeclaredInTask(task, parts[0]) == true) {
								// This part checks if the same LHS variable is
								// bound multiple times to several ports of the
								// same RHS task
								if (this.rhsInputBindings.put(parts[1],
										parts[0]) != null) {
									throw new Exception(
											"Error in rule: "
													+ ruleLine
													+ ". Duplicate attribute reference: '"
													+ parts[1] + "' for RHS: "
													+ list);
								}

							} else {
								// Handle the instance creation of RHS tasks
								if (parts[0].toLowerCase().compareTo(
										"instances") == 0) {
									try {
										Integer.parseInt(parts[1]);
									} catch (NumberFormatException nfe) {
										throw nfe;
									}
									this.tasksRHSBehaviour.put(task,
											new TaskBehaviour(parts[0],
													parts[1]));
								} else {
									throw new Exception("Error in rule: "
											+ ruleLine + ". Input attribute: "
											+ parts[0]
											+ " undeclared in Task: " + task);
								}
							}
						}
					} else {
						throw new Exception("Error in rule: " + ruleLine
								+ ". Invalid binding declaration (" + bindings
								+ ") for LHS or RHS: " + list + ". Missing =");
					}
				}
			} else {
				task = new String(item);
				tasks.add(task);
			}
			if (lhs == true) {
				if (this.taskLHSOutputBindings
						.put(task, this.lhsOutputBindings) != null) {
					throw new Exception("Error in rule: " + ruleLine
							+ ". Duplicate task: " + task + " in LHS: " + list);
				}
			} else {
				if (this.taskRHSInputBindings.put(task, this.rhsInputBindings) != null) {
					throw new Exception("Error in rule: " + ruleLine
							+ ". Duplicate task: " + task + " in RHS: " + list);
				}
			}

		}
		return tasks;
	}

	private boolean isOutputDeclaredInTask(String task, String attribute) {
		Enumeration<String> en = this.tasks.keys();
		String key = null;
		while (en.hasMoreElements()) {
			key = en.nextElement();
			if (key.compareTo(task) == 0) {
				break;
			}
		}
		if (key == null) {
			return false;
		}
		final TaskInfo ti = this.tasks.get(key);
		if (ti == null) {
			return false;
		}
		final Hashtable<String, String> outputs = ti.getOutput();

		if (outputs.get(attribute) == null) {
			return false;
		}
		return true;
	}

	private boolean isInputDeclaredInTask(String task, String attribute) {
		Enumeration<String> en = this.tasks.keys();
		String key = null;
		while (en.hasMoreElements()) {
			key = en.nextElement();
			if (key.compareTo(task) == 0) {
				break;
			}
		}
		if (key == null) {
			return false;
		}
		final TaskInfo ti = this.tasks.get(task);
		if (ti == null) {
			return false;
		}

		final Hashtable<String, String> outputs = ti.getInput();
		if (outputs.get(attribute) == null) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether a task is part of the <i>RHS</i> of a rule
	 * 
	 * @param rhs
	 * @param task
	 * @return
	 */
	private boolean isInLHS(ArrayList<String> lhs, String task) {
		for (String l : lhs) {
			if (task.compareTo(l) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This class hold information about the behaviour of a task. Correct
	 * behaviours include consuming (or not) task instances and creating one (or
	 * more) such instances
	 * 
	 * @author Marc Frincu
	 * @since 2009
	 * 
	 */
	public class TaskBehaviour {
		private String behaviourName, behaviourValue;

		TaskBehaviour(String behaviourName, String behaviourValue) {
			this.behaviourName = behaviourName;
			this.behaviourValue = behaviourValue;
		}

		public String getBehaviourName() {
			return this.behaviourName;
		}

		public String getBehaviourValue() {
			return this.behaviourValue;
		}
	}

	/**
	 * Returns a Hashtable<String, TaskInfo> containing tasks retrieved from the
	 * <i>SILK</i> file
	 * 
	 * @return the Hashtable with tasks
	 */
	public Hashtable<String, TaskInfo> getTasks() {
		return tasks;
	}

	/**
	 * Displays the content of the <i>SILK</i> file as a String
	 */
	public String toString() {
		return this.fileContent.toString();
	}
}
