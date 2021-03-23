/*
 * list utils
 */
package com.archeocomp.silentdrive.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * list utils
 */
public class ListUtils {
	
	/**
	 * 
	 * @param val
	 * @param len
	 * @return 
	 */
	public static<T> List<T> getListOf(T val, int len){
		return Stream.generate(String::new)
                       .limit(len)
                       .map(s -> val)
                       .collect(Collectors.toList());
	}
	
}
