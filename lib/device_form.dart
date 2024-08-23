import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'dart:io';

var userImage;

class RegistrationForm extends StatefulWidget {
  final TextEditingController nameController;
  final TextEditingController emiController;
  final TextEditingController adharController;
  final TextEditingController totalAmtController;
  final TextEditingController downPayment;
  final TextEditingController contact;
  final String? deviceName;
  final String? deviceModel;
  final String? imeiNumber;

  final int? selectedDuration;
  final Function(int?) onDurationChanged;
  final Function() onSubmit;

  RegistrationForm({
    Key? key,
    required this.nameController,
    required this.emiController,
    required this.adharController,
    required this.totalAmtController,
    required this.downPayment,
    required this.contact,
    this.deviceName,
    this.deviceModel,
    this.imeiNumber,
    this.selectedDuration,
    required this.onDurationChanged,
    required this.onSubmit,
  }) : super(key: key);

  @override
  _RegistrationFormState createState() => _RegistrationFormState();
}

class _RegistrationFormState extends State<RegistrationForm> {
  final _formKey = GlobalKey<FormState>();
  File? _image;
  String? _imageUrl;

  Future<void> _pickImage() async {
    final pickedFile =
        await ImagePicker().pickImage(source: ImageSource.camera);
    if (pickedFile != null) {
      setState(() {
        _image = File(pickedFile.path);
      });
      await _uploadImageToFirebase();
    }
  }

  Future<void> _uploadImageToFirebase() async {
    if (_image == null) return;

    try {
      final storageRef = FirebaseStorage.instance.ref();
      final imagesRef = storageRef.child(
          "avatars/${widget.nameController.text}_${DateTime.now().millisecondsSinceEpoch}.jpg");

      // Upload the file to Firebase Storage
      final uploadTask = imagesRef.putFile(
        _image!,
        SettableMetadata(contentType: 'image/jpeg'),
      );

      // Wait until the upload is complete
      final snapshot = await uploadTask.whenComplete(() {});

      // Get the download URL of the uploaded file
      final downloadUrl = await snapshot.ref.getDownloadURL();

      setState(() {
        _imageUrl = downloadUrl;
        userImage = downloadUrl;
      });

      print("Uploaded Image URL: $userImage}");
    } catch (e) {
      // Handle any errors that occur during upload
      print("Failed to upload image: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Register Device',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          SizedBox(height: 20.0),
          GestureDetector(
            onTap: _pickImage,
            child: CircleAvatar(
              radius: 50,
              backgroundImage: _image != null ? FileImage(_image!) : null,
              child: _image == null
                  ? Icon(Icons.camera_alt, size: 50, color: Colors.grey)
                  : null,
            ),
          ),
          if (_imageUrl != null)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Text(
                'Image uploaded successfully!',
                style: TextStyle(color: Colors.green),
              ),
            ),
          SizedBox(height: 20.0),
          TextFormField(
            controller: widget.nameController,
            decoration: InputDecoration(
              labelText: 'Name',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.person),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your name';
              }
              return null;
            },
          ),
          SizedBox(height: 10.0),
          TextFormField(
            controller: widget.contact,
            keyboardType: TextInputType.number,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly
            ],
            decoration: InputDecoration(
              labelText: 'Contact Number',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.phone),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your contact number';
              }
              return null;
            },
          ),
          SizedBox(height: 10.0),
          TextFormField(
            controller: widget.adharController,
            keyboardType: TextInputType.number,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly
            ],
            decoration: InputDecoration(
              labelText: 'Adhar',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.document_scanner),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your Adhar number';
              }
              return null;
            },
          ),
          SizedBox(height: 10.0),
          TextFormField(
            controller: widget.totalAmtController,
            keyboardType: TextInputType.number,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly
            ],
            decoration: InputDecoration(
              labelText: 'Device Price',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.currency_rupee),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter the total amount';
              }
              return null;
            },
          ),
          SizedBox(height: 10),
          TextFormField(
            controller: widget.downPayment,
            keyboardType: TextInputType.number,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly
            ],
            decoration: InputDecoration(
              labelText: 'Down Payment',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.payment),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter the down payment amount';
              }
              return null;
            },
          ),
          SizedBox(height: 10),
          TextFormField(
            controller: widget.emiController,
            keyboardType: TextInputType.number,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly
            ],
            decoration: InputDecoration(
              labelText: 'EMI Amount',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.attach_money),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter EMI amount';
              }
              if (double.tryParse(value) == null) {
                return 'Please enter a valid number';
              }
              return null;
            },
          ),
          SizedBox(height: 10.0),
          DropdownButtonFormField<int>(
            decoration: InputDecoration(
              labelText: 'EMI Duration',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              prefixIcon: Icon(Icons.punch_clock_outlined),
            ),
            value: widget.selectedDuration,
            onChanged: widget.onDurationChanged,
            items: [6, 8, 12].map((duration) {
              return DropdownMenuItem<int>(
                value: duration,
                child: Text('$duration months'),
              );
            }).toList(),
            validator: (value) {
              if (value == null) {
                return 'Please select EMI duration';
              }
              return null;
            },
          ),
          SizedBox(height: 20.0),
          if (widget.deviceName != null &&
              widget.deviceModel != null &&
              widget.imeiNumber != null)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Device Name: ${widget.deviceName}'),
                Text('Device Model: ${widget.deviceModel}'),
                Text('IMEI: ${widget.imeiNumber}'),
              ],
            ),
          SizedBox(height: 20.0),
          ElevatedButton(
            onPressed: () {
              if (_formKey.currentState!.validate()) {
                widget.onSubmit();
              }
            },
            child: Text('Submit'),
            style: ElevatedButton.styleFrom(
              padding:
                  const EdgeInsets.symmetric(horizontal: 32.0, vertical: 16.0),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
