package window.controller;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
//import com.sun.jna.*;
//import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.*;

public class GetWindowRect {

	public interface MyUser32 extends StdCallLibrary {
		MyUser32 INSTANCE = (MyUser32) Native.loadLibrary("user32", MyUser32.class,
				W32APIOptions.DEFAULT_OPTIONS);

		HWND FindWindow(String lpClassName, String lpWindowName);

		int GetWindowRect(HWND handle, int[] rect);
	}

	public static int[] getRect(String windowName) throws WindowNotFoundException,
	GetWindowRectException {

		
		HWND hwnd = MyUser32.INSTANCE.FindWindow(null, windowName);

		/*
		char[] buffer = new char[1000];
		
		//HWND next = User32.INSTANCE.GetForegroundWindow();
		HWND next = User32.INSTANCE.FindWindow(null, null);
		
		//HWND next = User32.INSTANCE.GetWindow(all, new DWORD(2));
		
		while (next != null) {
			
			User32.INSTANCE.GetWindowText(next, buffer, buffer.length);
			
			if (next != null) {
				System.out.println("found something - " + String.valueOf(buffer));
			}			
			
			next = User32.INSTANCE.GetWindow(next, new DWORD(2));
			
		}
		*/
		

		if (hwnd == null) {
			throw new WindowNotFoundException("", windowName);
		}

		int[] rect = {0, 0, 0, 0};
		int result = MyUser32.INSTANCE.GetWindowRect(hwnd, rect);
		if (result == 0) {
			throw new GetWindowRectException(windowName);
		}
		return rect;
	}

	@SuppressWarnings("serial")
	public static class WindowNotFoundException extends Exception {
		public WindowNotFoundException(String className, String windowName) {
			super(String.format("Window null for className: %s; windowName: %s", 
					className, windowName));
		}
	}

	@SuppressWarnings("serial")
	public static class GetWindowRectException extends Exception {
		public GetWindowRectException(String windowName) {
			super("Window Rect not found for " + windowName);
		}
	}
}