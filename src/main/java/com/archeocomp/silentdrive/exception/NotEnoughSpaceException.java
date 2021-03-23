/*
 * not enough space exception
 */
package com.archeocomp.silentdrive.exception;

/**
 * not enough space exception
 */
public class NotEnoughSpaceException extends RuntimeException {
	
	public NotEnoughSpaceException(String msg) {
		super(msg);
	}
}
