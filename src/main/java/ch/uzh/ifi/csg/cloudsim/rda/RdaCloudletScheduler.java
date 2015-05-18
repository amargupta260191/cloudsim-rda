package ch.uzh.ifi.csg.cloudsim.rda;

import java.util.List;

/**
 * This interface enhanced the VM scheduler with the necessary methods that are
 * required to be implemented when using the RDA module.
 * 
 * @author Patrick A. Taddei
 *
 */
public interface RdaCloudletScheduler {

	/**
	 * Updates the processing of cloudlets running under management of this
	 * scheduler.
	 * 
	 * @param currentTime
	 *            current simulation time
	 * @param mipsShare
	 *            list with MIPS share of each Pe available to the scheduler
	 * @param bwShare
	 *            bandwidth share available
	 * @param storageIOShare
	 *            storage IO share available
	 * 
	 * @return time predicted completion time of the earliest finishing cloudlet
	 */
	public double updateVmProcessing(double currentTime,
			List<Double> mipsShare, double bwShare, double storageIOShare);

	/**
	 * Returns the requested cpu utilization at the given time.
	 * 
	 * @param currentTime
	 *            current simulation time
	 * @return list of cpu cores with their utilization
	 */
	public List<Double> getCurrentRequestedMips(double currentTime);

	/**
	 * Returns the requested ram utilization at the given time.
	 * 
	 * @param currentTime
	 *            current simulation time
	 * @return requested ram
	 */
	public double getCurrentRequestedUtilizationOfRam(double currentTime);

	/**
	 * Returns the requested bandwidth utilization at the given time.
	 * 
	 * @param currentTime
	 *            current simulation time
	 * @return requested bandwidth
	 */
	public double getCurrentRequestedUtilizationOfBw(double currentTime);

	/**
	 * Returns the requested storage I/O utilization at the given time.
	 * 
	 * @param currentTime
	 *            current simulation time
	 * @return requested storage I/O
	 */
	public double getCurrentRequestedUtilizationOfStorageIO(double currentTime);

	/**
	 * The current requested gradient of the cpu.
	 * 
	 * @return gradient of the cpu
	 */
	public double getCurrentRequestedGradCpu();

	/**
	 * The current requested gradient of the bandwidth.
	 * 
	 * @return gradient of the bandwidth
	 */
	public double getCurrentRequestedGradBw();

	/**
	 * The current requested gradient of the storage I/O.
	 * 
	 * @return gradient of the storage I/O
	 */
	public double getCurrentRequestedGradStorageIO();

}
