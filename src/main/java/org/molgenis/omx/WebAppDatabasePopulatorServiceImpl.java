package org.molgenis.omx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.data.DataService;
import org.molgenis.data.annotation.impl.CaddServiceAnnotator;
import org.molgenis.data.annotation.impl.ClinVarServiceAnnotator;
import org.molgenis.data.annotation.impl.DbnsfpGeneServiceAnnotator;
import org.molgenis.data.annotation.impl.DbnsfpVariantServiceAnnotator;
import org.molgenis.data.annotation.provider.CgdDataProvider;
import org.molgenis.data.support.GenomeConfig;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.dataexplorer.controller.DataExplorerController;
import org.molgenis.framework.db.WebAppDatabasePopulatorService;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.auth.UserAuthority;
import org.molgenis.omx.controller.HomeController;
import org.molgenis.omx.core.RuntimeProperty;
import org.molgenis.security.MolgenisSecurityWebAppDatabasePopulatorService;
import org.molgenis.security.account.AccountService;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.runas.RunAsSystem;
import org.molgenis.studymanager.StudyManagerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebAppDatabasePopulatorServiceImpl implements WebAppDatabasePopulatorService
{
	private final DataService dataService;
	private final MolgenisSecurityWebAppDatabasePopulatorService molgenisSecurityWebAppDatabasePopulatorService;

    static final String KEY_APP_NAME = "app.name";
    static final String KEY_APP_HREF_LOGO = "app.href.logo";
    static final String KEY_APP_HREF_CSS = "app.href.css";

	@Autowired
	public WebAppDatabasePopulatorServiceImpl(DataService dataService,
			MolgenisSecurityWebAppDatabasePopulatorService molgenisSecurityWebAppDatabasePopulatorService)
	{
		if (dataService == null) throw new IllegalArgumentException("DataService is null");
		this.dataService = dataService;

		if (molgenisSecurityWebAppDatabasePopulatorService == null) throw new IllegalArgumentException(
				"MolgenisSecurityWebAppDatabasePopulator is null");
		this.molgenisSecurityWebAppDatabasePopulatorService = molgenisSecurityWebAppDatabasePopulatorService;
	}

	@Override
	@Transactional
	@RunAsSystem
	public void populateDatabase()
	{
		molgenisSecurityWebAppDatabasePopulatorService.populateDatabase(this.dataService, HomeController.ID);

		// Genomebrowser stuff
		Map<String, String> runtimePropertyMap = new HashMap<String, String>();

		runtimePropertyMap.put(DataExplorerController.INITLOCATION,
				"chr:'1',viewStart:10000000,viewEnd:10100000,cookieKey:'human',nopersist:true");
		runtimePropertyMap.put(DataExplorerController.COORDSYSTEM,
				"{speciesName: 'Human',taxon: 9606,auth: 'GRCh',version: '37',ucscName: 'hg19'}");
		runtimePropertyMap
				.put(DataExplorerController.CHAINS,
						"{hg18ToHg19: new Chainset('http://www.derkholm.net:8080/das/hg18ToHg19/', 'NCBI36', 'GRCh37',{speciesName: 'Human',taxon: 9606,auth: 'NCBI',version: 36,ucscName: 'hg18'})}");
		// for use of the demo dataset add to
		// SOURCES:",{name:'molgenis mutations',uri:'http://localhost:8080/das/molgenis/',desc:'Default from WebAppDatabasePopulatorService'}"
		runtimePropertyMap
				.put(DataExplorerController.SOURCES,
						"[{name:'Genome',twoBitURI:'http://www.biodalliance.org/datasets/hg19.2bit',tier_type: 'sequence'},{name: 'Genes',desc: 'Gene structures from GENCODE 19',bwgURI: 'http://www.biodalliance.org/datasets/gencode.bb',stylesheet_uri: 'http://www.biodalliance.org/stylesheets/gencode.xml',collapseSuperGroups: true,trixURI:'http://www.biodalliance.org/datasets/geneIndex.ix'},{name: 'Repeats',desc: 'Repeat annotation from Ensembl 59',bwgURI: 'http://www.biodalliance.org/datasets/repeats.bb',stylesheet_uri: 'http://www.biodalliance.org/stylesheets/bb-repeats.xml'},{name: 'Conservation',desc: 'Conservation',bwgURI: 'http://www.biodalliance.org/datasets/phastCons46way.bw',noDownsample: true}]");
		runtimePropertyMap
				.put(DataExplorerController.BROWSERLINKS,
						"{Ensembl: 'http://www.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'}");

		// include/exclude dataexplorer mods
		runtimePropertyMap.put(DataExplorerController.KEY_MOD_AGGREGATES, String.valueOf(true));
		runtimePropertyMap.put(DataExplorerController.KEY_MOD_CHARTS, String.valueOf(true));
		runtimePropertyMap.put(DataExplorerController.KEY_MOD_DATA, String.valueOf(true));
		runtimePropertyMap.put(DataExplorerController.KEY_MOD_DISEASEMATCHER, String.valueOf(false));
		runtimePropertyMap.put(DataExplorerController.KEY_MOD_ANNOTATORS, String.valueOf(false));

		// DataExplorer table editable yes/no
		runtimePropertyMap.put(DataExplorerController.KEY_DATAEXPLORER_EDITABLE, String.valueOf(false));
		runtimePropertyMap.put(DataExplorerController.KEY_GALAXY_ENABLED, String.valueOf(false));
		
		// DataExplorer rows clickable yes / no
		runtimePropertyMap.put(DataExplorerController.KEY_DATAEXPLORER_ROW_CLICKABLE, String.valueOf(false));

		// Annotators include files/tools
		String molgenisHomeDir = System.getProperty("molgenis.home");

		if (molgenisHomeDir == null)
		{
			throw new IllegalArgumentException("missing required java system property 'molgenis.home'");
		}

		if (!molgenisHomeDir.endsWith("/")) molgenisHomeDir = molgenisHomeDir + '/';
		String molgenisHomeDirAnnotationResources = molgenisHomeDir + "data/annotation_resources";

		runtimePropertyMap.put(CaddServiceAnnotator.CADD_FILE_LOCATION_PROPERTY, molgenisHomeDirAnnotationResources
				+ "/CADD/1000G.vcf.gz");
		runtimePropertyMap.put(CgdDataProvider.CGD_FILE_LOCATION_PROPERTY, molgenisHomeDirAnnotationResources
				+ "/CGD/CGD.txt");
		runtimePropertyMap.put(DbnsfpGeneServiceAnnotator.GENE_FILE_LOCATION_PROPERTY,
				molgenisHomeDirAnnotationResources + "/dbnsfp/dbNSFP2.3_gene");
		runtimePropertyMap.put(DbnsfpVariantServiceAnnotator.CHROMOSOME_FILE_LOCATION_PROPERTY,
				molgenisHomeDirAnnotationResources + "/dbnsfp/dbNSFP2.3_variant.chr");
		runtimePropertyMap.put(ClinVarServiceAnnotator.CLINVAR_FILE_LOCATION_PROPERTY,
				molgenisHomeDirAnnotationResources + "/Clinvar/variant_summary.txt");

		runtimePropertyMap.put(DataExplorerController.WIZARD_TITLE, "Filter Wizard");
		runtimePropertyMap.put(DataExplorerController.WIZARD_BUTTON_TITLE, "Wizard");

		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_START, "POS,start_nucleotide");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_STOP, "stop_pos,stop_nucleotide,end_nucleotide");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_CHROM, "CHROM,#CHROM,chromosome");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_ID, "ID,Mutation_id");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_DESCRIPTION, "INFO");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_PATIENT_ID, "patient_id");

        runtimePropertyMap.put(KEY_APP_NAME, "ASE Browser - MOLGENIS");
        runtimePropertyMap.put(KEY_APP_HREF_LOGO, "/img/umcg.png");
        runtimePropertyMap.put(KEY_APP_HREF_CSS, "ase.css");
        runtimePropertyMap.put(AccountService.KEY_PLUGIN_AUTH_ACTIVATIONMODE, "user");
        runtimePropertyMap
                .put("app.home",
                        "<div class=\"container-fluid\">"
                                + "<div class=\"row-fluid\">"
                                + "<div class=\"span6\">"
                                + "<h3>Welcome</h3>"
                                + "<p>Placeholder</p>"
                                + "</div>" + "<div class=\"span6\">"
                                + "</div>" + "</div>");

        for (Entry<String, String> entry : runtimePropertyMap.entrySet())
        {
            RuntimeProperty runtimeProperty = new RuntimeProperty();
            String propertyKey = entry.getKey();
            runtimeProperty.setIdentifier(RuntimeProperty.class.getSimpleName() + '_' + propertyKey);
            runtimeProperty.setName(propertyKey);
            runtimeProperty.setValue(entry.getValue());
            dataService.add(RuntimeProperty.ENTITY_NAME, runtimeProperty);
        }

        MolgenisUser anonymousUser = molgenisSecurityWebAppDatabasePopulatorService.getAnonymousUser();
		UserAuthority anonymousASEAuthority = new UserAuthority();
		anonymousASEAuthority.setMolgenisUser(anonymousUser);
        anonymousASEAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX + "ase".toUpperCase());
		dataService.add(UserAuthority.ENTITY_NAME, anonymousASEAuthority);

        UserAuthority anonymousSamplesAuthority = new UserAuthority();
        anonymousSamplesAuthority.setMolgenisUser(anonymousUser);
        anonymousSamplesAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX + "Samples".toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousSamplesAuthority);

        UserAuthority anonymousAseEntityAuthority = new UserAuthority();
        anonymousAseEntityAuthority.setMolgenisUser(anonymousUser);
        anonymousAseEntityAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX + "ase".toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousAseEntityAuthority);

        UserAuthority anonymousSamplesEntityAuthority = new UserAuthority();
        anonymousSamplesEntityAuthority.setMolgenisUser(anonymousUser);
        anonymousSamplesEntityAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX + "samples".toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousSamplesEntityAuthority);

        UserAuthority anonymousDataExplorerAuthority = new UserAuthority();
        anonymousDataExplorerAuthority.setMolgenisUser(anonymousUser);
        anonymousDataExplorerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX + DataExplorerController.ID.toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousDataExplorerAuthority);

        UserAuthority anonymousHomeAuthority = new UserAuthority();
        anonymousHomeAuthority.setMolgenisUser(anonymousUser);
        anonymousHomeAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX + HomeController.ID.toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousHomeAuthority);

        UserAuthority anonymousRTPAuthority = new UserAuthority();
        anonymousRTPAuthority.setMolgenisUser(anonymousUser);
        anonymousRTPAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX + RuntimeProperty.ID.toUpperCase());
        dataService.add(UserAuthority.ENTITY_NAME, anonymousRTPAuthority);
	}

	@Override
	@Transactional
	@RunAsSystem
	public boolean isDatabasePopulated()
	{
		return dataService.count(MolgenisUser.ENTITY_NAME, new QueryImpl()) > 0;
	}
}