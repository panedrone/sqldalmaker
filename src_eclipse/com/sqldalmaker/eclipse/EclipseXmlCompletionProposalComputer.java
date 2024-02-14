/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseXmlCompletionProposalComputer implements ICompletionProposalComputer {

	// http://stackoverflow.com/questions/306713/collections-emptylist-returns-a-listobject
	//
	private final List<ICompletionProposal> NONE = Collections.<ICompletionProposal>emptyList();

	@Override
	public List<ICompletionProposal> computeCompletionProposals(CompletionProposalInvocationContext context,
			IProgressMonitor monitor) {

		try {
			ITextViewer text_viewer = context.getViewer();
			if (text_viewer == null) {
				return NONE;
			}
			int cursor_offset = context.getInvocationOffset();
			IFile this_xml_file = EclipseXmlAttrHelpers.get_current_file();
			if (this_xml_file == null) {
				return NONE;
			}
			boolean is_sdm_xml = FileSearchHelpers.is_sdm_xml(this_xml_file.getName());
			boolean is_dao_xml = FileSearchHelpers.is_dao_xml(this_xml_file.getName());
			if (!is_sdm_xml && !is_dao_xml) {
				return NONE;
			}
			IDocument doc = text_viewer.getDocument();
			if (doc == null) {
				return NONE;
			}
			String text = doc.get();
			IRegion region = EclipseXmlAttrHelpers.get_attribute_value_region(cursor_offset, text);
			if (region == null) {
				return NONE;
			}
			String value;
			try {
				value = text.substring(region.getOffset(), region.getOffset() + region.getLength());
			} catch (Throwable e) {
				return NONE;
			}
			int offset_inside_value = cursor_offset - region.getOffset();
			String qualifier;
			if (offset_inside_value < 0) {
				return NONE;
			} else if (offset_inside_value == 0) {
				qualifier = "";
			} else {
				qualifier = value.substring(0, offset_inside_value);
			}
			int attr_offset = region.getOffset() - 2;
			if (EclipseXmlAttrHelpers.is_value_of("ref", attr_offset, text)) {
				// === panedrone: this is incorrect condition because
				// "dict/" is correct qualifier
				// for dict/get_categories.sql
				// if (value.toLowerCase().endsWith(".sql") == false) {
				// return NONE;
				// }
				IContainer this_folder = this_xml_file.getParent();
				IResource root = EclipseTargetLanguageHelpers.find_root_file(this_folder);
				String xml_metaprogram_folder_path = root.getParent().getLocation().toPortableString();
				Settings settings = SdmUtils.load_settings(xml_metaprogram_folder_path);
				String sql_root_rel_path = settings.getFolders().getSql();
				IProject project = this_xml_file.getProject();
				IFolder sql_root = project.getFolder(sql_root_rel_path);
				if (sql_root == null) {
					return NONE;
				}
				List<String> list = new ArrayList<String>();
				enum_sql_files(sql_root, list, sql_root.getFullPath().toPortableString());
				List<ICompletionProposal> prop_list = new ArrayList<ICompletionProposal>();
				compute_structure_proposals(qualifier, cursor_offset, prop_list, list);
				return prop_list;
			}
			if (EclipseXmlAttrHelpers.is_value_of("dto", attr_offset, text)) {
				IContainer metaprogram_folder = this_xml_file.getParent();
				IResource res = metaprogram_folder.findMember(Const.SDM_XML);
				if (!(res instanceof IFile)) {
					return NONE;
				}
				IFile sdm_xml_file = (IFile) res;
				String xml;
				InputStream stream = sdm_xml_file.getContents();
				try {
					xml = input_stream_to_string(stream);
				} finally {
					stream.close();
				}
				List<String> list = get_attribute_value("name", xml);
				List<ICompletionProposal> prop_list = new ArrayList<ICompletionProposal>();
				compute_structure_proposals(qualifier, cursor_offset, prop_list, list);
				return prop_list;
			} else if (is_dao_xml) {
				if (EclipseXmlAttrHelpers.is_value_of("table", attr_offset, text)) {
					IContainer metaprogram_folder = this_xml_file.getParent();
					IResource res = metaprogram_folder.findMember(Const.SDM_XML);
					if (!(res instanceof IFile)) {
						return NONE;
					}
					IFile sdm_xml_file = (IFile) res;
					String xml;
					InputStream stream = sdm_xml_file.getContents();
					try {
						xml = input_stream_to_string(stream);
					} finally {
						stream.close();
					}
					List<String> list_fillterd = new ArrayList<String>();
					List<String> list = get_attribute_value("ref", xml);
					for (String s : list) {
						if (s.endsWith(".sql") == false) {
							list_fillterd.add(s);
						}
					}
					List<ICompletionProposal> prop_list = new ArrayList<ICompletionProposal>();
					compute_structure_proposals(qualifier, cursor_offset, prop_list, list_fillterd);
					return prop_list;
				}
			}
			return NONE;
		} catch (Throwable e) {
			e.printStackTrace();
			// EclipseConsoleHelpers.init_console();
			// EclipseConsoleHelpers.add_error_msg(this.getClass().getName(),
			// e.getMessage());
			return NONE;
		}
	}

	// http://stackoverflow.com/questions/17586789/how-to-extract-attribute-values-with-xpath
	//
	private static List<String> get_attribute_value(String attribute, String xml_response_body) {
		List<String> results = new ArrayList<String>();
		try {
			DocumentBuilderFactory doc_builder_factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder doc_builder = doc_builder_factory.newDocumentBuilder();
			Document doc = doc_builder.parse(new InputSource(new StringReader(xml_response_body)));
			XPathFactory xpath_factory = XPathFactory.newInstance();
			XPath xpath = xpath_factory.newXPath();
			try {
				XPathExpression expr = xpath.compile("//@" + attribute);
				Object result = expr.evaluate(doc, XPathConstants.NODESET);
				NodeList node_list = (NodeList) result;
				for (int i = 0; i < node_list.getLength(); i++) {
					results.add(node_list.item(i).getTextContent());
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}

	// http://howtodoinjava.com/2013/10/06/how-to-read-data-from-inputstream-into-string-in-java/
	//
	public static String input_stream_to_string(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			StringBuilder out = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				out.append(line);
			}
			return out.toString();
		} finally {
			reader.close();
		}
	}

	private static void enum_sql_files(IFolder dir, List<String> res, String sql_root_rel_path) throws Exception {
		if (dir.exists() == false) { // e.g. incorrect value in settings.xml
			return;
		}
		IResource[] members = dir.members();
		for (IResource r : members) {
			if (r instanceof IFolder) {
				enum_sql_files((IFolder) r, res, sql_root_rel_path);
			} else if (r instanceof IFile) {
				String rel_path = ((IFile) r).getFullPath().toPortableString();
				if (rel_path.endsWith(".sql")) {
					int start = sql_root_rel_path.length() + 1;
					String path = rel_path.substring(start);
					res.add(path);
				}
			}
		}
	}

	// ===: implementation of computeStructureProposals is based on
	// http://www.ibm.com/developerworks/library/os-ecca/
	//
	private static void compute_structure_proposals(String qualifier, int doc_offset,
			List<ICompletionProposal> prop_list, List<String> complete_List) {

		int qlen = qualifier.length();
		// Loop through all proposals
		for (int i = 0; i < complete_List.size(); i++) {
			String text = complete_List.get(i);
			// Check if proposal matches qualifier
			if (text.startsWith(qualifier)) { // ===: "___".startsWith("") is true
				// Derive cursor position
				int cursor = text.length();
				CompletionProposal proposal = new CompletionProposal(text, doc_offset - qlen, qlen, cursor);
				prop_list.add(proposal);
			}
		}
	}

	@Override
	public List<ICompletionProposal> computeContextInformation(CompletionProposalInvocationContext context,
			IProgressMonitor monitor) {
		return NONE;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
	}

	@Override
	public void sessionStarted() {
	}
}