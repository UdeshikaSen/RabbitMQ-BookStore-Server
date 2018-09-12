package com.bookstore.server

import net.liftweb.json.Extraction.decompose
import net.liftweb.json.JsonAST.compactRender

object BookController {

  // add a new book to the store
  def addBook(bookId: String, bookName: String, bookType: String): Book = {
    try {
      val newBook = new Book(bookId, bookName, bookType)
      BookStore.bookList.put(bookId, newBook)
      return newBook
    }
    catch {
      case ex: Exception => null
    }
  }

  // get details of a particular book
  def getBookDetails(bookId: String): Book = {
    try {
      val retrievedBook: Book = BookStore.bookList(bookId)
      retrievedBook
    }
    catch {
      case ex: NoSuchElementException => null
    }
  }

  // get all books
  def getBooks(): String = {
    implicit val formats = net.liftweb.json.DefaultFormats
    var response = compactRender(decompose(BookStore.bookList))

    return response
  }
}
