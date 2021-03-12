package pdm000;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultSetToListServiceImp {

	/**
	 * SELECT * FROM websites 查詢所有記錄，以List返回 list物件的每一個元素都是一條記錄
	 * 每條記錄儲存在Map<String, Object>裡面，String型別指欄位名字，Object對應欄位值
	 * 
	 * @param rs
	 * @return List<Map<String, Object>>
	 */
	public List<Map<String, Object>> selectAll(ResultSet rs) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			// 獲取結果集結構（元素據）
			ResultSetMetaData rmd = rs.getMetaData();
			// 獲取欄位數（即每條記錄有多少個欄位）
			int columnCount = rmd.getColumnCount();
			while (rs.next()) {
				// 儲存記錄中的每個<欄位名-欄位值>
				Map<String, Object> rowData = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; ++i) {
					// <欄位名-欄位值>
					rowData.put(rmd.getColumnName(i), rs.getObject(i));
				}
				// 獲取到了一條記錄，放入list
				list.add(rowData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return list;
	}
}