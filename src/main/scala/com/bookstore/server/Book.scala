package com.bookstore.server

class Book(val bookId: String, val bookName: String, val bookType: String) {

  override def toString: String = {
    "Book Id : " + bookId + "\n" + " Name : " + bookName + "\n" + " Type: " + bookType
  }
}
