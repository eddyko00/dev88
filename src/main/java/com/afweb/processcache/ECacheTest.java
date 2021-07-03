package com.afweb.processcache;

/**
 * test for ECache
 * @author lixk
 */
public class ECacheTest {
	public static void main(String[] args) throws InterruptedException {
		ECache.put("id", 123);
		Integer id = ECache.get("id");
		System.out.println(id);

		ECache.put("id", 123, 3000);
		id = ECache.get("id");
		System.out.println(id);
		Thread.sleep(3001);
		id = ECache.get("id");
		System.out.println("expired id:"+id);
	}
}
