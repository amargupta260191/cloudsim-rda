package ch.uzh.ifi.csg.cloudsim.rda.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;

import ch.uzh.ifi.csg.cloudsim.rda.RdaCloudlet;
import ch.uzh.ifi.csg.cloudsim.rda.RdaCloudletSchedulerDynamicWorkload;
import ch.uzh.ifi.csg.cloudsim.rda.RdaVm;
import ch.uzh.ifi.csg.cloudsim.rda.greediness.RdaHostGreedinessUserAware;
import ch.uzh.ifi.csg.cloudsim.rda.greediness.VmSchedulerGreedinessAllocationAlgorithmUserAware;
import ch.uzh.ifi.csg.cloudsim.rda.provisioners.BwProvisionerSimple;
import ch.uzh.ifi.csg.cloudsim.rda.provisioners.RamProvisionerSimple;
import ch.uzh.ifi.csg.cloudsim.rda.provisioners.StorageIOProvisionerSimple;
import ch.uzh.ifi.csg.cloudsim.rda.useraware.UserAwareDatacenter;

/**
 * A simple example showing how to create a data center with one host and run
 * one cloudlet on it.
 */
public class GreedinessExampleUserAware {
	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;
	/** The vmlist. */
	private static List<Vm> vmlist;

	private static double schedulingInterval = 0.000000001;
	private static double scarcitySchedulingInterval = 0.01; // milli second
	private static String pythonPath = "C:\\Program Files (x86)\\Python34\\python";

	public static boolean record = false;

	
	/**
	 * Creates main() to run this example.
	 *
	 * @param args
	 *            the args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Log.printLine("Starting ...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 2; // number of cloud users
			Calendar calendar = Calendar.getInstance(); // Calendar whose fields
														// have been initialized
														// with the current date
														// and time.
			boolean trace_flag = false; // trace events

			/*
			 * Comment Start - Dinesh Bhagwat Initialize the CloudSim library.
			 * init() invokes initCommonVariable() which in turn calls
			 * initialize() (all these 3 methods are defined in CloudSim.java).
			 * initialize() creates two collections - an ArrayList of SimEntity
			 * Objects (named entities which denote the simulation entities) and
			 * a LinkedHashMap (named entitiesByName which denote the
			 * LinkedHashMap of the same simulation entities), with name of
			 * every SimEntity as the key. initialize() creates two queues - a
			 * Queue of SimEvents (future) and another Queue of SimEvents
			 * (deferred). initialize() creates a HashMap of of Predicates (with
			 * integers as keys) - these predicates are used to select a
			 * particular event from the deferred queue. initialize() sets the
			 * simulation clock to 0 and running (a boolean flag) to false. Once
			 * initialize() returns (note that we are in method
			 * initCommonVariable() now), a CloudSimShutDown (which is derived
			 * from SimEntity) instance is created (with numuser as 1, its name
			 * as CloudSimShutDown, id as -1, and state as RUNNABLE). Then this
			 * new entity is added to the simulation While being added to the
			 * simulation, its id changes to 0 (from the earlier -1). The two
			 * collections - entities and entitiesByName are updated with this
			 * SimEntity. the shutdownId (whose default value was -1) is 0 Once
			 * initCommonVariable() returns (note that we are in method init()
			 * now), a CloudInformationService (which is also derived from
			 * SimEntity) instance is created (with its name as
			 * CloudInformatinService, id as -1, and state as RUNNABLE). Then
			 * this new entity is also added to the simulation. While being
			 * added to the simulation, the id of the SimEntitiy is changed to 1
			 * (which is the next id) from its earlier value of -1. The two
			 * collections - entities and entitiesByName are updated with this
			 * SimEntity. the cisId(whose default value is -1) is 1 Comment End
			 * - Dinesh Bhagwat
			 */
			CloudSim.init(num_user, calendar, trace_flag, schedulingInterval);

			// Second step: Create Datacenters
			// Datacenters are the resource providers in CloudSim. We need at
			// list one of them to run a CloudSim simulation
			Datacenter datacenter0 = createDatacenter("Datacenter_01");

			// Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Fourth step: Create one virtual machine
			vmlist = new ArrayList<Vm>();

			// VM description
			int vmid = 0;
			int mips = 500;
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 500;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			Vm vm = new RdaVm(vmid, brokerId, mips, pesNumber, ram, bw, size,
					1, vmm, new RdaCloudletSchedulerDynamicWorkload(mips,
							pesNumber), schedulingInterval);
			((RdaVm) vm).setOwner("user1");
			vmlist.add(vm);

			vm = new RdaVm(1, brokerId, mips, pesNumber, ram, bw, size, 1, vmm,
					new RdaCloudletSchedulerDynamicWorkload(mips, pesNumber),
					schedulingInterval);
			((RdaVm) vm).setOwner("user2");
			vmlist.add(vm);

			// submit vm list to the broker
			broker.submitVmList(vmlist);

			// Fifth step: Create one Cloudlet
			cloudletList = new ArrayList<Cloudlet>();

			// Cloudlet properties
			int id = 0;
			long length = 4000;
			long fileSize = 300;
			long outputSize = 300;

			Cloudlet cloudlet = new RdaCloudlet(
					1,
					pesNumber,
					fileSize,
					outputSize,
					"src\\main\\resources\\input1.csv",
					record);
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(0);
			cloudletList.add(cloudlet);

			cloudlet = new RdaCloudlet(
					2,
					pesNumber,
					fileSize,
					outputSize,
					"src\\main\\resources\\input2.csv",
					record);
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(1);
			cloudletList.add(cloudlet);

			// cloudletList.add(cloudlet);

			// submit cloudlet list to the broker
			broker.submitCloudletList(cloudletList);

			// Sixth step: Starts the simulation
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);

			Log.printLine("Test finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the datacenter.
	 *
	 * @param name
	 *            the name
	 *
	 * @return the datacenter
	 */
	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store
		// our machine
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores.
		// In this example, it will have only one core.
		List<Pe> peList = new ArrayList<Pe>();

		int mips = 1000;

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store
																// Pe id and
																// MIPS Rating

		// 4. Create Host with its id and list of PEs and add them to the list
		// of machines
		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 1000;

		RamProvisionerSimple ramProvisioner = new RamProvisionerSimple(ram);
		BwProvisionerSimple bwProvisioner = new BwProvisionerSimple(bw);
		StorageIOProvisionerSimple storageIO = new StorageIOProvisionerSimple(
				10000);
		hostList.add(new RdaHostGreedinessUserAware(hostId, ramProvisioner,
				bwProvisioner, storageIO, storage, peList,
				new VmSchedulerGreedinessAllocationAlgorithmUserAware(peList,
						ramProvisioner, bwProvisioner, storageIO, pythonPath),
				scarcitySchedulingInterval)); // This
		// is
		// our
		// machine

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are
																		// not
																		// adding
																		// SAN
		// devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new UserAwareDatacenter(name, characteristics,
					new PowerVmAllocationPolicySimple(hostList), storageList,
					schedulingInterval);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	// We strongly encourage users to develop their own broker policies, to
	// submit vms and cloudlets according
	// to the specific rules of the simulated scenario
	/**
	 * Creates the broker.
	 *
	 * @return the datacenter broker
	 */
	private static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects.
	 *
	 * @param list
	 *            list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + "Time"
				+ indent + indent + indent + indent + "Start Time" + indent
				+ "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getVmId()
						+ indent + indent
						+ Math.round(cloudlet.getActualCPUTime() * 1000000)
						/ 1000000.0 + indent + indent
						+ dft.format(cloudlet.getExecStartTime()) + indent
						+ indent + dft.format(cloudlet.getFinishTime()));
			}

			// Log.printLine(cloudlet.getCloudletHistory());

		}
	}
}