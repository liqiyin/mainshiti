### 链接

<https://www.jianshu.com/p/97503d7faaf3>

---

有些时候，我们需要在两个Activity之间，Activity和Service之间等等的情况下传递一些信息，如果要传递的数据是基本数据类型，这个方法是没有问题的。假如我们要传递的不是基本数据类型，是一个Java中的一个类的对象（比如：Calendar对象），或者是一个我们自定义的对象，那应该怎么办？
有两个解决方案：
* 第一个是使用Serializable接口，这个接口是Java SE本身就支持的序列化接口，但是使用这个接口来进行Intent数据的传递有一个缺点。因为这个序列化和反序列化过程中需要大量I/O操作，从而导致开销大效率低。
* 第二个是使用Parcelable接口的使用。这种方式是Android中支持的序列化方式，使用起来稍微麻烦点，但是效率更高，所以我们一般使用这个方式来通过Intent传递数据。

### Parcelable接口的基本使用方法

在需要进行传递的类中实现Parcelable接口，假如我们要传递一个Person类的对象,我们需要在其中实现Parcelable接口：

```java
public class Person implements Parcelable{
    private String name;
    private Int age;

    //下面是实现Parcelable接口的内容
    @Override
    public int describeContents() {
        //一般返回零就可以了
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {      
        //在这个方法中写入这个类的变量
        //对应着 String name;
        dest.writeString();
        //对应着 Int age;
        dest.writeInt();
    }

    //在实现上面的接口方法后，接下来还需要执行反序列化，定义一个变量，并重新定义其中的部分方法
    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>(){

        @Override
        public Person createFromParcel(Parcel source) {                  
            // 在这个方法中反序列化上面的序列化内容，最后根据反序列化得到的各个属性，得到之前试图传递的对象
            // 反序列化的属性的顺序必须和之前写入的顺序一致
            Person person = new Person();
            person.name = source.readString();
            person.age = source.readAge();
            return person;
        }

        @Override
        public Person[] newArray(int size) {
            //一般返回一个数量为size的传递的类的数组就可以了
            return new Person[size];
        }
    };
}
```

---

### 使用Parcelable接口的注意事项

boolean类型的序列化和反序列化。由于没有直接序列化boolean类型的方式，所以采用如下方式：

```java
//序列化
dest.writeByte((Byte)(isChecked ? 1 : 0));

//反序列化
XXX.isChecked = source.readByte != 0;
```

---

### 当类中某一个属性也是一个自定义类时

一种解决方案就是将该类也实现Parcelalbe接口。

如果传递的对象的某个属性所属的类是系统自带的同时没有也不可能实现Parcelable接口的类（比如：Calendar），或者是一个第三方类，但是不方便实现Parcelable接口，那么可以在将该对象拆为若干个必要的属性进行传递，反序列化后再组装。

```java
public class Person implements Parcelable{
      ... //一些属性
      Calendar calendar;
      ...
      //序列化Calendar对象，Calendar对象中最重要的就是Time和TimeZone两个属性，所以可以将其拆为这两个属性进行序列化
      dest.writeLong(calendar.getTimeInMillis());
      dest.writeInt(calendar.getTimeZone().getID());

      //进行反序列化
      person.calendar.setTimeInMillis(source.readLong());
      person.calendar.getTimeZone().setID(source.readString());
}
```

---

### 如果有一个属性是Enum数组

```java
public class Person implements Parcelable {
      ...//其他属性
      private Day[] days = {Day.SUNDAY,Day.MONDAY,Day.TUESDAY,Day.WEDNSDAY,Day.THURSDAY,Day.FRIDAY,Day.SATURDAY};

      //进行序列化，将Enum数组转换成Int类型的List,利用每个Enum对象都有自己的需要order()
      List<Integer> dayInts = new ArrayList<Integer>();
       for (Day tempDay : days){
           dayInts.add(tempDay.ordinal());
       }
      dest.writeList(dayInts);

      //进行反序列化，将上面的过程反过来
      List<Integer> dayInts = new ArrayList<Integer>();
      source.readList(dayInts,null);
      List<Day> dayEnums = new ArrayList<Day>();
      for(int temp : dayInts){
            dayEnums.add(Day.values()[temp]);
      }
      XXX.days = daysEnums.toArray(new Day[dayEnum.size()]);
}
```

---

### 选择序列化方法的原则

1. 在使用内存的时候，Parcelable比Serializable性能高，所以推荐使用Parcelable。
2. Serializable在序列化的时候会产生大量的临时变量，从而引起频繁的GC。
3. Parcelable不能使用在要将数据存储在磁盘上的情况，因为Parcelable不能很好的保证数据的持续性在外界有变化的情况下。尽管Serializable效率低点，但此时还是建议使用Serializable 。
