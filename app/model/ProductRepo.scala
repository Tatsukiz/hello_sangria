package model

class ProductRepo {
  private val Products = List(
    Product("1", "CheeseCake", "Tasty"),
    Product("2", "Health Potion", "+50 HP"))

  def product(id: String): Option[Product] =
    Products find (_.id == id)

  def products: List[Product] = Products
}
