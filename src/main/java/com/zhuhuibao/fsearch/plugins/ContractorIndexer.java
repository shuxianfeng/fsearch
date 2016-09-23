package com.zhuhuibao.fsearch.plugins;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;

import com.zhuhuibao.fsearch.G;
import com.zhuhuibao.fsearch.service.MemberService;
import com.zhuhuibao.fsearch.service.dao.MemberDao;
import com.zhuhuibao.fsearch.util.Factor;
import com.zhuhuibao.fsearch.util.FormulaUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.Indexer;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.core.SearcherOptions;

public class ContractorIndexer implements Indexer {

	@Override
	public void init(SearcherOptions options, PropertiesConfig config) throws Exception {

	}

	@Override
	public Path fullIndex(Searcher searcher) throws Exception {
		SearcherOptions options = searcher.getOptions();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		File dir = new File(options.getPath() + "-" + dateFormat.format(new Date()));
		boolean mkdirs = dir.mkdirs();
		Path path = dir.toPath();
		L.warn("ContractorIndexer::fullIndex::mkdirs=" + mkdirs + "::path=" + path.toString());
		Directory directory = SimpleFSDirectory.open(path);

		try {
			int batch = 2000;
			Object lastId = null;
			JdbcTemplate template = DataSourceManager.getJdbcTemplate();
			int total = 0;
			while (true) {
				Object[] params = null;
				String sql = "select m.id as id, m.mobile as mobile, m.email as email, m.registerTime as registerTime, if(m.status=10,'已认证','未认证') authinfo,"
						+ " m.enterpriseName as enterpriseName, m.province as province, m.address as address, m.enterpriseType as enterpriseType,"
						+ " m.enterpriseLogo as enterpriseLogo, m.enterpriseDesc as enterpriseDesc, m.saleProductDesc as saleProductDesc, m.employeeNumber as employeeNumber,"
						+ " m.enterpriseWebSite as enterpriseWebSite, m.enterpriseLinkman as enterpriseLinkman, v.vip_level as vip_level"
						+ " from t_m_member as m left join t_vip_member_info as v on v.member_id=m.id"
						+ " where m.status not in (0,2) and m.workType=100 and m.enterpriseEmployeeParentId=0 and m.identify like '%6%'"
						+ " ";
				if (lastId != null) {
					params = new Object[] { lastId };
					sql += " and m.id>?";
				}
				sql += " order by id asc";
				List<Map<String, Object>> docs = template.findList(sql, params, 0, batch, MapHandler.CASESENSITIVE);
				if (docs.isEmpty()) {
					break;
				}

				Object nextLastId = docs.get(docs.size() - 1).get("id");
				Map certMap = this.findCertMsap(template, lastId, nextLastId);

				lastId = nextLastId;
				List<Document> documents = new ArrayList<>(docs.size());
				for (Map<String, Object> docAsMap : docs) {
					Long id = FormatUtil.parseLong(docAsMap.get("id"));

					List<Map<String, Object>> assetlevels = (List<Map<String, Object>>) certMap.get(id.toString());

					// if (id == 140209 || id == 140217) {
					// L.error(id + ">>>" + assetlevels.size());
					// }

					if (CollectionUtil.isNotEmpty(assetlevels)) {
						for (Map<String, Object> map : assetlevels) {

							String assetlevel = FormatUtil.parseString(map.get("certificate_name"));
							if (map.get("certificate_grade") != null) {
								assetlevel += FormatUtil.parseString(map.get("certificate_grade"));
								String certificateName = FormatUtil.parseString(map.get("certificate_name"));
								docAsMap.put(certificateName, certificateName);
							}
							docAsMap.put(assetlevel, assetlevel);

						}
						if (L.isInfoEnabled()) {
							L.info(this.getClass() + "-----------caijl:contractor.assetlevels= "
									+ StringUtil.join(assetlevels, ","));
						}
						genCertLevel(docAsMap, assetlevels);

					} else {
						docAsMap.put("certLevel", 0);
					}

					Map<String, Object> doc = parseRawDocument(docAsMap);
					Document document = searcher.parseDocument(doc);

					if (L.isInfoEnabled()) {
						L.info(this.getClass() + " saving document: " + document);
					}
					documents.add(document);
				}
				IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(Searcher.ANALYZER));
				writer.addDocuments(documents);
				writer.close();
				total += documents.size();
			}
			L.warn(this.getClass() + " saved documents: " + total);
			return path;
		} catch (Exception e) {
			FileUtil.delete(path.toFile());
			L.error("执行异常>>>", e);
			throw e;
		} finally {
			FileUtil.close(directory);
		}
	}

	private Map findCertMsap(JdbcTemplate template, Object lastId, Object nextLastId) throws Exception {
		Object[] params = null;
		String sql = "select c.id as id, c.certificate_name as certificate_name, c.certificate_grade as certificate_grade, c.mem_id as mem_id"
				+ " from t_m_member as m, t_certificate_record as c"
				+ " where m.status not in (0,2) and m.workType=100 and m.enterpriseEmployeeParentId=0 and m.identify like '%6%'"
				+ " and m.id=c.mem_id";
		if (lastId != null) {
			params = new Object[] { lastId, nextLastId };
			sql += " and m.id>? and m.id<=?";
		}
		List<Map<String, Object>> docs = template.findList(sql, params, 0, Integer.MAX_VALUE, MapHandler.CASESENSITIVE);

		Map m = new HashMap();
		for (Map doc : docs) {
			this.addToCertMap(doc, m);
		}
		return m;
	}

	private void addToCertMap(Map doc, Map m) {
		Long mem_id = FormatUtil.parseLong(doc.get("mem_id"));
		List list = (List) m.get(mem_id.toString());
		if (null == list) {
			list = new ArrayList();
			m.put(mem_id.toString(), list);
		}
		list.add(doc);
	}

	/**
	 * 排序规则
	 *
	 * @param docAsMap
	 * @param assetlevels
	 */
	private void genCertLevel(Map<String, Object> docAsMap, List<Map<String, Object>> assetlevels) {
		double certLevel;

		// Long id = FormatUtil.parseLong(docAsMap.get("id"));

		if (CollectionUtil.isNotEmpty(assetlevels)) {
			List<Factor> list = new ArrayList<>();
			for (Map<String, Object> map : assetlevels) {
				// 企业资质权重=∑（资质权重×资质对应的“资质等级权重”）
				double certWeight, gradeWeight;
				String certName = FormatUtil.parseString(map.get("certificate_name"));
				String grade = FormatUtil.parseString(map.get("certificate_grade"));
				double certDefalutWeight = G.getConfig().getDouble("其他");
				if (StringUtil.isNotEmpty(certName.trim())) {
					certWeight = G.getConfig().getDouble(certName.trim(), certDefalutWeight);
				} else {
					certWeight = certDefalutWeight;
				}

				double gradeDefaultWeight = G.getConfig().getDouble("空值");
				if (null != grade && StringUtil.isNotEmpty(grade.trim())) {
					gradeWeight = G.getConfig().getDouble(grade.trim(), gradeDefaultWeight);
				} else {
					gradeWeight = gradeDefaultWeight;
				}

				// if (id == 140209 || id == 140217) {
				// L.error("[" + certName + "]:" + certWeight + " * " + "[" +
				// grade + "]:" + gradeWeight);
				// }

				Factor factor = new Factor();
				factor.setWeight(certWeight);
				factor.setScore(gradeWeight);
				list.add(factor);
			}
			certLevel = FormulaUtil.calWeight(list);

			// if (id == 140209 || id == 140217) {
			// L.error("\t>>>{" + docAsMap.get("enterpriseName") + "}权重值:\t" +
			// certLevel);
			// }

			docAsMap.put("certLevel", certLevel);
		} else {
			docAsMap.put("certLevel", 0);
		}
	}

	@Override
	public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap) throws Exception {
		MemberService memberService = new MemberService();
		String registerTime = FormatUtil.parseString(docAsMap.get("registerTime"));
		memberService.analysTime(docAsMap, registerTime);

		// viplevel
		Object vip = docAsMap.get("vip_level");
		docAsMap.put("viplevel", (null != vip) ? FormatUtil.parseString(vip) : "");

		// add viplevel to certLevel
		double vipNum = 100;
		if (null != vip) {
			vipNum = Double.parseDouble(FormatUtil.parseString(vip));
		}
		double certLevel = ((Number) docAsMap.get("certLevel")).doubleValue();
		docAsMap.put("certLevel", certLevel + vipNum * 10000);

		return docAsMap;
	}

}
