# Clone Utils

This utility class clones and patches POJOs my using Jackson's object-mapper.

## Usage

Clone object:

```
MyObject cloned = CloneUtils.deepClone(new MyObject());
```

Clone object to different type:

```
MyOtherType cloned = CloneUtils.deepClone(new MyObject(), MyOtherType.class);
```

Clone and patch object:

```
MyObject patchedClone = CloneUtils.deepClone(new MyObject(), new MyPatch());
```

Clone and patch object to different type:

```
MyOtherType patchedClone = CloneUtils.deepClone(new MyObject(), new MyPatch(), MyOtherType.class);
```

## Build

```bash
$ mvn package
```
