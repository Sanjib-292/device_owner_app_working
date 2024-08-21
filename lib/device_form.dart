import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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

  const RegistrationForm({
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
              prefixIcon: Icon(Icons.document_scanner),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your Adhar number';
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
              labelText: 'Emi Duration',
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
