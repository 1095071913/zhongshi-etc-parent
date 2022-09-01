/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.jiumao.generate.code;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jiumao.generate.code.entity.DataSourceConfig;
import com.jiumao.generate.code.entity.EntityData;
import com.jiumao.generate.code.entity.TableData;
import com.jiumao.generate.code.parent.GenerateParentPom;
import com.jiumao.generate.code.parent.dto.GenerateDtoPom;
import com.jiumao.generate.code.parent.enums.GenerateEnumPom;
import com.jiumao.generate.code.parent.rest.GenerateRestPom;
import com.jiumao.generate.code.parent.rpc.GenerateRpcPom;
import com.jiumao.generate.code.parent.service.GenerateEntity;
import com.jiumao.generate.code.parent.service.GenerateMapper;
import com.jiumao.generate.code.parent.service.GenerateMapperXML;
import com.jiumao.generate.code.parent.service.GenerateProperties;
import com.jiumao.generate.code.parent.service.GenerateRun;
import com.jiumao.generate.code.parent.service.GenerateService;
import com.jiumao.generate.code.parent.service.GenerateServiceImpl;
import com.jiumao.generate.code.parent.service.GenerateServicePom;
import com.jiumao.generate.code.parent.vo.GenerateVoPom;
import com.jiumao.generate.code.tool.SQLType;

/**	
 * 
 *  Specifications：功能
 * 
 *  Author：彭晋龙
 * 
 *  Creation Date：2021-12-18:16:32:34
 *
 *  Copyright Ownership：xiao mao zi
 * 
 *  Agreement That：Apache 2.0
 * 
 */

public class GenerateCodeRun {
	
	public static void main(String[] args) throws Exception {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print("请输入数据库地址如 （localhost:3306/zhongshi-test-db）：");
		DataSourceConfig.JDBCURL = bufferedReader.readLine();
		
		System.out.print("请输入数据库用户名：");
		DataSourceConfig.USER = bufferedReader.readLine();
		
		System.out.print("请输入数据库密码：");
		DataSourceConfig.PASSWORD = bufferedReader.readLine();
		
		System.out.print("请输入模块名字（userss）：");
		String mobule = bufferedReader.readLine();
		
		System.out.print("请输入包名（com.jiumao）：");
		String packageName = bufferedReader.readLine();
		
		System.out.print("请输入生成地址（D:\\project\\jiumao-saas-parent\\jiumao-saas-common）：");
		String pash = bufferedReader.readLine();
		
		Map<String,List<TableData>> moduleTableNameMap = new HashMap<String, List<TableData>>(){{
		
			for(;;) {
				
				System.out.print("请输入子模块名（输入break结束）：");
				String childModule = bufferedReader.readLine();
				if(childModule.equals("break")) {
					break;
				}
				
				put(childModule, new ArrayList<TableData>() {{
					
					for(;;) {
						
						System.out.print("请输入表名（输入break结束）：");
						String tableName = bufferedReader.readLine();
						if(tableName.equals("break")) {
							break;
						}
						
						System.out.print("请输入表名实体映射过滤前缀：");
						String prefixFilter = bufferedReader.readLine();
						
						add(new TableData(tableName,prefixFilter));
						
					}
					
				}});
				
			}
			
		}};
		
		generateCode(mobule,moduleTableNameMap,packageName,pash);
		
	}

	
	
	
	
	private static void generateCode(String module,Map<String,List<TableData>> moduleTableNameMap,String packageName,String pash) throws Exception {
		
		System.out.println("正在生成中   。。。。。。");
		
		pash += "\\jiumao-saas-"+module;
		String voPash = pash+"\\jiumao-saas-"+module+"-vo";
		String dtoPash = pash+"\\jiumao-saas-"+module+"-dto";
		String enumPash = pash+"\\jiumao-saas-"+module+"-enum";
		String servicePash = pash+"\\jiumao-saas-service-"+module;
		String rpcPash = pash+"\\jiumao-saas-service-rpc-api-"+module;
		String restPash = pash+"\\jiumao-saas-service-rest-api-"+module;
		
		
		GenerateParentPom.generate(module, pash);
		GenerateVoPom.generate(module, voPash);
		GenerateDtoPom.generate(module, dtoPash);
		GenerateEnumPom.generate(module, enumPash);
		GenerateRpcPom.generate(module, rpcPash);
		GenerateRestPom.generate(module, restPash);
		GenerateServicePom.generate(module, servicePash);
		
		
		List<EntityData> entityData = GenerateEntity.generate(module,moduleTableNameMap, packageName, servicePash+ "\\src\\main\\java");
		GenerateRun.generate(module,packageName, servicePash+ "\\src\\main\\java");
		GenerateProperties.generate(module, servicePash+ "\\src\\main\\resources");
		GenerateMapper.generate(module,entityData, servicePash+ "\\src\\main\\java");
		GenerateMapperXML.generate(module,entityData, servicePash+ "\\src\\main\\resources");
		GenerateService.generate(module,entityData, servicePash+ "\\src\\main\\java");
		GenerateServiceImpl.generate(module,entityData, servicePash+ "\\src\\main\\java");
		
		
		
		System.out.println(pash);
		
		for(String childModule : moduleTableNameMap.keySet()) {
			
			SQLType.fileCreate(servicePash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\tool", null,null);
			SQLType.fileCreate(servicePash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\config", null,null);
			SQLType.fileCreate(servicePash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\properties", null,null);
			
			
			SQLType.fileCreate(restPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rest\\v1\\app", null,null);
			SQLType.fileCreate(restPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rest\\v1\\pc", null,null);
			SQLType.fileCreate(restPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rest\\v2\\app", null,null);
			SQLType.fileCreate(restPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rest\\v2\\pc", null,null);
			
			SQLType.fileCreate(rpcPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rpc\\v1", null,null);
			SQLType.fileCreate(rpcPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\api\\rpc\\v2", null,null);
			
			SQLType.fileCreate(enumPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\enums", null,null);
			
			SQLType.fileCreate(dtoPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\dto\\v1\\app", null,null);
			SQLType.fileCreate(dtoPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\dto\\v1\\pc", null,null);
			SQLType.fileCreate(dtoPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\dto\\v2\\app", null,null);
			SQLType.fileCreate(dtoPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\dto\\v2\\pc", null,null);
			
			SQLType.fileCreate(voPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\vo\\v1\\app", null,null);
			SQLType.fileCreate(voPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\vo\\v1\\pc", null,null);
			SQLType.fileCreate(voPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\vo\\v2\\app", null,null);
			SQLType.fileCreate(voPash+"\\src\\main\\java\\com\\jiumao\\"+module+"\\"+childModule+"\\vo\\v2\\pc", null,null);
			
		}
		
		
		Runtime runtime = Runtime.getRuntime();
		
		runtime.exec("cmd /c start explorer "+pash);

		System.out.println("代码生成完成   。。。。。");
		
	}

}