// TrainManagerXml.java

package jmri.jmrit.operations.trains;

import java.io.File;
import java.util.List;

import jmri.jmrit.XmlFile;
import jmri.jmrit.operations.setup.Control;
import jmri.jmrit.operations.setup.Setup;
import jmri.jmrit.operations.OperationsXml;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;

/**
 * Loads and stores trains using xml files. Also stores various train
 * parameters managed by the TrainManager.
 * 
 * @author Daniel Boudreau Copyright (C) 2008, 2010
 * @version $Revision$
 */
public class TrainManagerXml extends OperationsXml {
	
	private boolean fileLoaded = false;
	
	public TrainManagerXml(){
	}
	
	/** record the single instance **/
	private static TrainManagerXml _instance = null;

	public static synchronized TrainManagerXml instance() {
		if (_instance == null) {
			if (log.isDebugEnabled()) log.debug("TrainManagerXml creating instance");
			// create and load
			_instance = new TrainManagerXml();
			_instance.load();
		}
		if (Control.showInstance && log.isDebugEnabled()) log.debug("TrainManagerXml returns instance "+_instance);
		return _instance;
	}
	
	public void writeFile(String name) throws java.io.FileNotFoundException, java.io.IOException {
		if (log.isDebugEnabled()) log.debug("writeFile "+name);
		// This is taken in large part from "Java and XML" page 368
		File file = findFile(name);
		if (file == null) {
			file = new File(name);
		}
		// create root element
		Element root = new Element("operations-config");
		Document doc = newDocument(root, dtdLocation+"operations-trains.dtd");

		// add XSLT processing instruction
		java.util.Map<String, String> m = new java.util.HashMap<String, String>();
		m.put("type", "text/xsl");
		m.put("href", xsltLocation+"operations-trains.xsl");
		ProcessingInstruction p = new ProcessingInstruction("xml-stylesheet", m);
		doc.addContent(0,p);

		// Inspect the comment to change any \n characters to <?p?> processor
		TrainManager manager = TrainManager.instance();
		List<String> trainList = manager.getTrainsByIdList();

		// add top-level elements      
		root.addContent(manager.store());
		root.addContent(TrainScheduleManager.instance().store());
		Element values;
		root.addContent(values = new Element("trains"));
		// add entries
		for (int i=0; i<trainList.size(); i++) {
			String trainId = trainList.get(i);
			Train train = manager.getTrainById(trainId);
			train.setComment(convertToXmlComment(train.getComment()));
			values.addContent(train.store());
		}
		writeXML(file, doc);

		//Now that the roster has been rewritten in file form we need to
		//restore the RosterEntry object to its normal \n state for the
		//comment fields.
		for (int i=0; i<trainList.size(); i++){
			Train train = manager.getTrainById(trainList.get(i));
			train.setComment(convertFromXmlComment(train.getComment()));
		}
		// done - train file now stored, so can't be dirty
		setDirty(false);
	}

    
    /**
     * Read the contents of a roster XML file into this object. Note that this does not
     * clear any existing entries.
     */
    public void readFile(String name) throws org.jdom.JDOMException, java.io.IOException {
    	
    	TrainManager manager = TrainManager.instance();
    	
    	// suppress rootFromName(name) warning message by checking to see if file exists
    	if (findFile(name) == null) {
    		log.debug(name + " file could not be found");
    		fileLoaded = true;	// set flag, could be the first time
    		return;
    	}
    	// find root
    	Element root = rootFromName(name);
    	if (root==null) {
    		log.debug(name + " file could not be read");
    		return;
    	}
    	//if (log.isDebugEnabled()) XmlFile.dumpElement(root);

    	if (root.getChild("options") != null) {
    		Element e = root.getChild("options");
    		manager.options(e);
    	}
    	
    	TrainScheduleManager.instance().load(root);

    	if (root.getChild("trains") != null) {
    		@SuppressWarnings("unchecked")
    		List<Element> l = root.getChild("trains").getChildren("train");
    		if (log.isDebugEnabled()) log.debug("readFile sees "+l.size()+" trains");
    		for (int i=0; i<l.size(); i++) {
    			manager.register(new Train(l.get(i)));
    		}

    		fileLoaded = true;	// set flag

    		List<String> trainList = manager.getTrainsByIdList();

    		// load train icon if needed, and convert comments
    		for (int i = 0; i < trainList.size(); i++) {
    			//Get a RosterEntry object for this index
    			Train train = manager.getTrainById(trainList.get(i));
    			train.setComment(convertFromXmlComment(train.getComment()));
    			train.loadTrainIcon();
    		}
    		// loading complete run startup scripts
    		manager.runStartUpScripts();
    	}
    	else {
    		log.error("Unrecognized operations train file contents in file: "+name);
    	}
		log.debug("Trains have been loaded!");
		TrainLogger.instance().enableTrainLogging(Setup.isTrainLoggerEnabled());
		setDirty(false);	// clear dirty flag
    }
    
    public boolean isTrainFileLoaded(){
    	return fileLoaded;
    }

	/**
     * Store the train's build status
     */
    public File createTrainBuildReportFile(String name) {
    	return createFile(defaultBuildReportFilename(name), false);	// don't backup
	}
    
    public File getTrainBuildReportFile(String name) {
    	File file = new File(defaultBuildReportFilename(name));
    	return file;
    }
     
    public String defaultBuildReportFilename(String name) { 
    	return XmlFile.prefsDir()+OperationsXml.getOperationsDirectoryName()+File.separator+"buildstatus"+File.separator+BuildReportFileName+name+fileType;
    }
    public void setBuildReportName(String name) { BuildReportFileName = name; }
    private String BuildReportFileName = "train (";
    private String ManifestFileName = "train (";
    private String fileType =").txt";
    private String fileTypeCsv =").csv";
    
	/**
     * Store the train's manifest
     */
    public File createTrainManifestFile(String name) {
    	return createFile(defaultManifestFilename(name), false);	// don't backup
	}
    
    public File getTrainManifestFile(String name) {
    	File file = new File(defaultManifestFilename(name));
    	return file;
    }
     
    public String defaultManifestFilename(String name) { 
    	return XmlFile.prefsDir()+OperationsXml.getOperationsDirectoryName()+File.separator+"manifests"+File.separator+ManifestFileName+name+fileType;
    }
    
    public File getTrainCsvManifestFile(String name) {
    	File file = new File(defaultCsvManifestFilename(name));
    	return file;
    }
     
    
    public File createTrainCsvManifestFile(String name) {
    	return createFile(defaultCsvManifestFilename(name), false);	// don't backup
    }
    
    public String defaultCsvManifestFilename(String name) { 
    	return XmlFile.prefsDir()+OperationsXml.getOperationsDirectoryName()+File.separator+"csvManifests"+File.separator+ManifestFileName+name+fileTypeCsv;
    }

 
    
	/**
     * Store the switch list for a location
     */
    public File createSwitchListFile(String name) {
    	return createFile(defaultSwitchListName(name), false);	// don't backup
	}
     
    public File getSwitchListFile(String name) {
    	File file = new File(defaultSwitchListName(name));
    	return file;
    }
     
    public String defaultSwitchListName(String name) { 
    	return XmlFile.prefsDir()+OperationsXml.getOperationsDirectoryName()+File.separator+"switchLists"+File.separator+SwitchListFileName+name+fileType;
    }
    
	/**
     * Store the csv switch list for a location
     */
    public File createCsvSwitchListFile(String name) {
    	return createFile(defaultCsvSwitchListName(name), false);	// don't backup
	}
     
    public File getCsvSwitchListFile(String name) {
    	File file = new File(defaultCsvSwitchListName(name));
    	return file;
    }
     
    public String defaultCsvSwitchListName(String name) { 
    	return XmlFile.prefsDir()+OperationsXml.getOperationsDirectoryName()+File.separator+"csvSwitchLists"+File.separator+SwitchListFileName+name+fileTypeCsv;
    }
    
    public void setTrainSwitchListName(String name) { SwitchListFileName = name; }
    private String SwitchListFileName = "location (";
    
    public void setOperationsFileName(String name) { operationsFileName = name; }
	public String getOperationsFileName(){
		return operationsFileName;
	}

    private String operationsFileName = "OperationsTrainRoster.xml";

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TrainManagerXml.class.getName());

}
