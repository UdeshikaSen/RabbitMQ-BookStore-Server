package com.bookstore.server

import com.rabbitmq.client.{AMQP, ConnectionFactory, DefaultConsumer, Envelope}

object Server {

  private val REQUEST_QUEUE_A = "AddBook"
  private val REQUEST_QUEUE_B = "GetBook"
  private val REQUEST_QUEUE_C = "GetBooks"

  private val RESPONSE_QUEUE = "BookStore Server Response"

  def main(args: Array[String]): Unit = {

    val factory = new ConnectionFactory()
    factory.setHost("localhost")
    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null)
    channel.queueDeclare(REQUEST_QUEUE_A, false, false, false, null)
    channel.queueDeclare(REQUEST_QUEUE_B, false, false, false, null)
    channel.queueDeclare(REQUEST_QUEUE_C, false, false, false, null)

    println("Awaiting for requests.... ")

    // Callback consumer - to consume the AddBook request message published by client
    val addBookConsumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {

        var response: String = ""

        val bookDetails = new String(body, "UTF-8")
        val bookDetailsArray: Array[String] = bookDetails.split(",")
        val addedBook: Book = BookController.addBook(bookDetailsArray(0), bookDetailsArray(1), bookDetailsArray(2))
        if (addedBook != null) {
          response = "Book " + addedBook.bookName + "(" + addedBook.bookId + ") added to the book store"
        }
        else {
          response = "Some error occurred in adding the book to the store "
        }

        // Publish a response for the AddBook request sent from the client
        val replyProperties = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId).build()
        // properties.getReplyTo -> call back queue where the response message is published
        channel.basicPublish("", properties.getReplyTo, replyProperties, response.getBytes("UTF-8"))
        channel.basicAck(envelope.getDeliveryTag, false)
      }
    }

    // Callback consumer - to consume the GetBook request message published by client
    val getBookConsumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        var response: String = ""

        val bookId = new String(body, "UTF-8")
        val retrievedBook: Book = BookController.getBookDetails(bookId)
        if (retrievedBook != null) {
          response = "Book Id : " + retrievedBook.bookId + "\nName : " + retrievedBook.bookName + "\nType : " + retrievedBook.bookType
        }
        else {
          response = "Invalid Book Id"
        }

        // Publish a response for the GetBook request sent from the client
        val replyProperties = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId).build()
        // properties.getReplyTo -> call back queue where the response message is published
        channel.basicPublish("", properties.getReplyTo, replyProperties, response.getBytes("UTF-8"))
        channel.basicAck(envelope.getDeliveryTag, false)
      }
    }

    // Callback consumer - to consume the GetBooks request message published by client
    val getBooksConsumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        var response: String = ""

        val jsonResponse: String = BookController.getBooks()
        if (jsonResponse != null && jsonResponse != "[]") {
          response = jsonResponse
        }
        else {
          response = "Books not returned from store"
        }

        // Publish a response for the GetBooks request sent from the client
        val replyProperties = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId).build()
        // properties.getReplyTo -> call back queue where the response message is published
        channel.basicPublish("", properties.getReplyTo, replyProperties, response.getBytes("UTF-8"))
        channel.basicAck(envelope.getDeliveryTag, false)
      }
    }

    // Consume the AddBook request
    channel.basicConsume(REQUEST_QUEUE_A, false, addBookConsumer)
    // Consume the GetBook request
    channel.basicConsume(REQUEST_QUEUE_B, false, getBookConsumer)
    // Consume the GetBooks request
    channel.basicConsume(REQUEST_QUEUE_C, false, getBooksConsumer)
  }
}
