package com.bookstore.server

import java.io.IOException

import com.rabbitmq.client.{AMQP, ConnectionFactory, DefaultConsumer, Envelope}

object Server {

  private val REQUEST_QUEUE_A = "GetBook"
  private val REQUEST_QUEUE_B = "AddBook"
  private val REQUEST_QUEUE_C = "GetBooks"

  def main(args: Array[String]): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost("localhost")
    val connection = factory.newConnection()
    val channel = connection.createChannel()
    channel.queueDeclare(REQUEST_QUEUE_A, false, false, false, null)

    //channel.basicQos(1)
    println(" Awaiting for requests. To exit press CTRL+C")

    // callback consumer
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {

        // get Book
        val bookId = new String(body, "UTF-8")
        var response: String = ""
        val retrievedBook: Book = BookController.getBookDetails(bookId)
        if (retrievedBook != null) {
          response = "Book Id : " + retrievedBook.bookId + "\nName : " + retrievedBook.bookName + "\nType : " + retrievedBook.bookType
        }
        else {
          response = "Invalid Book Id"
        }
        val replyProperties = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId).build()
        // properties.getReplyTo -> call back queue where the response message is sent
        channel.basicPublish("", properties.getReplyTo, replyProperties, response.getBytes("UTF-8"))
//        channel.basicPublish("", "hello", replyProperties, response.getBytes("UTF-8"))

        channel.basicAck(envelope.getDeliveryTag, false)

        // RabbitMq consumer worker thread notifies the RPC server owner thread
        this synchronized this.notify()
      }
    }

    // consume the request to get a book
    channel.basicConsume(REQUEST_QUEUE_A, true, consumer)


    // wait to consume messages from RPC client
    //        while (true) {
    //      synchronized(consumer) {
    //            try {
    //              consumer.wait()
    //            }
    //            catch {
    //              case e: InterruptedException => e.printStackTrace()
    //            }
    //      }
    //      }

    //    if (connection != null) {
    //      try {
    //        connection.close()
    //      }
    //      catch {
    //        case e: IOException =>
    //      }
    //
    //    }

  }
}
