package server;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Servlet implementation class UploadFile
 */
@WebServlet("/UploadFile")
public class UploadFile extends HttpServlet {

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UploadFile() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		getFile(request,response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	private void getFile(HttpServletRequest request, HttpServletResponse response) {
		// 设置工厂
		DiskFileItemFactory factory = new DiskFileItemFactory();
		String path = request.getSession().getServletContext().getRealPath("/upload");
		File file = new File(path);
		if(!file.exists()){
			file.mkdirs();
		}
		// 设置文件存储位置
		factory.setRepository(new File(path));
		// 设置大小，如果文件小于设置大小的话，放入内存中，如果大于的话则放入磁盘中
		factory.setSizeThreshold(1024 * 1024);

		ServletFileUpload upload = new ServletFileUpload(factory);
		// 这里就是中文文件名处理的代码，其实只有一行，serheaderencoding就可以了
		upload.setHeaderEncoding("utf-8");
		/*
		 * String enCoding = request.getCharacterEncoding(); if(enCoding !=
		 * null){ upload.setHeaderEncoding(enCoding); }
		 */

		try {
			List<FileItem> list = upload.parseRequest(request);
			for (FileItem item : list) {
				// 判断是不是上传的文件，如果不是得到值，并设置到request域中
				// 这里的item.getfieldname是得到上传页面上的input上的name
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString("utf-8");
					System.out.println(name);
					System.out.println(value);
					request.setAttribute(name, value);
				}
				// 如果是上传的文件，则取出文件名，
				else {
					String name = item.getFieldName();
					String value = item.getName();
					System.out.println(name);
					System.out.println(value);
					// 得到不要地址的文件名，不同的浏览器传递的参数不同，有的直接传递文件名，而又的把文件地址一起传递过来
					// 使用substring方法可以统一得到文件名而不得到文件位置
					String fileName = value;
					request.setAttribute(name, fileName);
					// 写文件到path目录，文件名问filename
					item.write(new File(path, fileName));
				}
			}
		}

		catch (FileUploadException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
