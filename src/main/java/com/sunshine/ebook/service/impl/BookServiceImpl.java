package com.sunshine.ebook.service.impl;

import java.util.HashMap;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sunshine.ebook.entity.Book;
import com.sunshine.ebook.mapper.BookMapper;
import com.sunshine.ebook.service.BookService;

@Service("bookService")
public class BookServiceImpl implements BookService {
	
	@Autowired
	private BookMapper bookMapper;

	@Override
	public Book getBookinfoByCondition(HashMap<String, Object> map) {
		return bookMapper.getBookinfoByCondition(map);
	}

	@Override
	public boolean saveBookinfo(HashMap<String, Object> map) {
		boolean flag = false;
		try {
			bookMapper.saveBookinfo(map);
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	public void updateBookinfo(HashMap<String, Object> map) {
		bookMapper.updateBookinfo(map);
	}

	@Override
	public Page<Book> queryBookList(String bookName, String author, Integer categoryid, int startPage, int pageSize) {
		PageHelper.startPage(startPage, pageSize, true);
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (null != bookName || !"".equals(bookName)) {
			map.put("name", bookName);
		}
		if (null != author || !"".equals(author)) {
			map.put("author", author);
		}
		if (null != categoryid || !"".equals(categoryid)) {
			map.put("categoryid", categoryid);
		}
		return bookMapper.queryBookList(map);
	}

}
