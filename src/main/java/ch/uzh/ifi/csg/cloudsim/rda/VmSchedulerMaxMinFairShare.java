package ch.uzh.ifi.csg.cloudsim.rda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;

import ch.uzh.ifi.csg.cloudsim.rda.provisioners.BwProvisioner;
import ch.uzh.ifi.csg.cloudsim.rda.provisioners.RamProvisioner;
import ch.uzh.ifi.csg.cloudsim.rda.provisioners.StorageIOProvisioner;

/**
 * This VM scheduler is the standard VM scheduler to be used within the RDA
 * module.
 *  <br/> <br/>
 * This scheduler uses the Max-Min Fair Share (MMFS) algorithm to allocate the
 * resource to the different VMs. The MMFS algorithm is the basic way, how
 * multiple processes on one host are sharing the physical resources.
 * <br/> <br/>
 * How does it work:
 *  <br/> <br/>
 * 1. Check which resource is the most scarce of the resources cpu, bandwidth
 * and storage I/O.
 *  <br/> <br/>
 * 2. Downgrade that resource for all VMs
 *  <br/> <br/>
 * 3. Downgrade all the other resources of each VM. According to the Leontief
 * production function dependencies.
 *  <br/> <br/>
 * 4. Repeat the step 1-3 for the remaining resources.
 *  <br/> <br/>
 * This procedure guarantees that the host resources are not overused.
 *  <br/> <br/>
 * @author Patrick A. Taddei
 * @see MaxMinAlgorithm
 */
public class VmSchedulerMaxMinFairShare extends VmSchedulerTimeShared implements
		RdaVmScheduler {

	private MaxMinAlgorithm maxMin = new MaxMinAlgorithm();

	private RamProvisioner ramProvisioner;
	private BwProvisioner bwProvisioner;
	private StorageIOProvisioner sProvisioner;

	/**
	 * Instantiates a new vm scheduler time shared.
	 * 
	 * @param pelist
	 *            the pelist
	 * @param ramProvisioner
	 *            the ram provisioner
	 * @param bwProvisioner
	 *            the bandwidth provisioner
	 * @param sProvisioner
	 *            the storage I/O provisioner
	 */
	public VmSchedulerMaxMinFairShare(List<? extends Pe> pelist,
			RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			StorageIOProvisioner storageIOProvisioner) {
		super(pelist);
		this.ramProvisioner = ramProvisioner;
		this.bwProvisioner = bwProvisioner;
		this.sProvisioner = storageIOProvisioner;

	}

	/**
	 * Please read class description above for further details.
	 * 
	 * @param currentTime
	 *            the simulation time
	 * @param vms
	 *            the VMs to allocate the resources to
	 */
	public void allocateResourcesForAllVms(double currentTime, List<Vm> vms) {

		super.getMipsMap().clear();
		setAvailableMips(getMipsCapacity());

		bwProvisioner.deallocateBwForAllVms();
		ramProvisioner.deallocateRamForAllVms();
		sProvisioner.deallocateStorageIOForAllVms();

		HashMap<String, Double> requestedCpu = new HashMap<String, Double>();
		HashMap<String, Double> requestedRam = new HashMap<String, Double>();
		HashMap<String, Double> requestedBw = new HashMap<String, Double>();
		HashMap<String, Double> requestedStorageIO = new HashMap<String, Double>();

		double totReqRam = 0.0;

		for (Vm vm : vms) {
			double reqRam = ((RdaVm) vm).getCurrentRequestedRam(currentTime);
			double reqBw = ((RdaVm) vm).getCurrentRequestedBw(currentTime);
			double reqStorage = ((RdaVm) vm)
					.getCurrentRequestedStorageIO(currentTime);
			double reqCpu = ((RdaVm) vm)
					.getCurrentRequestedTotalMips(currentTime);

			String uid = (String) vm.getUid();

			requestedCpu.put(uid, reqCpu);
			requestedRam.put(uid, reqRam);
			requestedBw.put(uid, reqBw);
			requestedStorageIO.put(uid, reqStorage);

			totReqRam += reqRam;

		}

		if (totReqRam > ramProvisioner.getRam()) {
			throw new RuntimeException(
					"Requested RAM is more than available RAM. ");
		}

		double mipsCapacity = getMipsCapacity();

		Map<String, Double> allocatedCpu = null;
		Map<String, Double> allocatedBw = null;
		Map<String, Double> allocatedStorageIO = null;

		// check which resource is the most scarce resource
		double demandCpu = maxMin.getResourceDemand(requestedCpu, mipsCapacity);
		double demandBw = maxMin.getResourceDemand(requestedBw,
				bwProvisioner.getBw());
		double demandStorageIO = maxMin.getResourceDemand(requestedStorageIO,
				sProvisioner.getStorageIO());

		for (int i = 0; i < 3; i++) {
			// if the demand for CPU has the highest percentage
			if (demandCpu >= demandBw && demandCpu >= demandStorageIO) {
				allocatedCpu = maxMin.evaluate(requestedCpu, mipsCapacity);
				for (Vm vm : vms) {
					String uid = (String) vm.getUid();
					double dampingFactor = requestedCpu.get(uid)
							/ allocatedCpu.get(uid);
					if (dampingFactor != 0 && !Double.isNaN(dampingFactor)) {
						double bw = requestedBw.get(uid);
						requestedBw.put(uid, bw / dampingFactor);
						double storageIO = requestedStorageIO.get(uid);
						requestedStorageIO.put(uid, storageIO / dampingFactor);
					}
				}
				demandCpu = -1;
			}
			// if the demand for BW has the highest percentage
			else if (demandBw >= demandStorageIO) {
				allocatedBw = maxMin.evaluate(requestedBw,
						bwProvisioner.getBw());
				for (Vm vm : vms) {
					String uid = (String) vm.getUid();
					double dampingFactor = requestedBw.get(uid)
							/ allocatedBw.get(uid);
					if (dampingFactor != 0 && !Double.isNaN(dampingFactor)) {
						double cpu = requestedCpu.get(uid);
						requestedCpu.put(uid, cpu / dampingFactor);
						double storageIO = requestedStorageIO.get(uid);
						requestedStorageIO.put(uid, storageIO / dampingFactor);
					}

				}
				demandBw = -1;
			}
			// if the demand for storage has the highest percentage
			else {
				allocatedStorageIO = maxMin.evaluate(requestedStorageIO,
						sProvisioner.getStorageIO());
				for (Vm vm : vms) {
					String uid = (String) vm.getUid();
					double dampingFactor = requestedStorageIO.get(uid)
							/ allocatedStorageIO.get(uid);
					if (dampingFactor != 0 && !Double.isNaN(dampingFactor)) {
						double cpu = requestedCpu.get(uid);
						requestedCpu.put(uid, cpu / dampingFactor);
						double bw = requestedBw.get(uid);
						requestedBw.put(uid, bw / dampingFactor);
					}

				}
				demandStorageIO = -1;
			}
		}

		for (Vm vm : vms) {

			double mips = allocatedCpu.get(vm.getUid());

			if (super.getAvailableMips() - mips < -0.1) {
				throw new RuntimeException(
						"Trying to allocate more MIPS than available.");
			}

			List<Double> mipsMapCapped = new ArrayList<Double>();
			// split the mips equally between all processor units
			int peCnt = vm.getNumberOfPes();
			for (int n = 0; n < peCnt; n++) {
				mipsMapCapped.add((double) (mips / peCnt));
			}

			getMipsMap().put(vm.getUid(), mipsMapCapped);
			setAvailableMips(super.getAvailableMips() - mips);

			String uid = (String) vm.getUid();
			((RdaVm) vm).setCurrentAllocatedMips(mipsMapCapped);

			double ram = requestedRam.get(uid);
			double bw = allocatedBw.get(uid);
			double storageIO = allocatedStorageIO.get(uid);

			bwProvisioner.allocateBwForVm(vm, bw);
			ramProvisioner.allocateRamForVm(vm, ram);
			sProvisioner.allocateStorageIOForVm((RdaVm) vm, storageIO);
		}
	}

	/**
	 * Returns total MIPS among all the PEs.
	 * 
	 * @return mips capacity
	 */
	private double getMipsCapacity() {
		if (getPeList() == null) {
			Log.printLine("Pe list is empty");
			return 0;
		}

		double capacity = 0.0;
		for (Pe pe : getPeList()) {
			capacity += pe.getMips();
		}

		return capacity;
	}
}
