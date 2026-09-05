import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../constants/api_constants.dart';
import '../errors/exceptions.dart';

/// Central HTTP client used by all feature datasources.
///
/// Encapsulates base URL, headers, authentication tokens, timeouts and error
/// handling so that feature datasources never duplicate API configuration.
///
/// This is infrastructure: it must not contain business logic. The client is
/// intentionally coupled to `package:http` — the [ApiClient] interface is the
/// seam that datasources depend on, so the underlying HTTP implementation can
/// be swapped without touching feature code.
class ApiClient {
  ApiClient({
    http.Client? httpClient,
    String? baseUrl,
    this.tokenProvider,
  })  : _httpClient = httpClient ?? http.Client(),
        _baseUrl = baseUrl ?? ApiConstants.baseUrl;

  final http.Client _httpClient;
  final String _baseUrl;

  /// Supplies an optional bearer token for authenticated requests.
  final Future<String?> Function()? tokenProvider;

  /// Performs a GET request and decodes the JSON response.
  Future<dynamic> get(
    String path, {
    Map<String, String>? queryParameters,
  }) {
    return _send('GET', path, queryParameters: queryParameters);
  }

  /// Performs a POST request with an optional JSON body.
  Future<dynamic> post(
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) {
    return _send('POST', path, body: body, queryParameters: queryParameters);
  }

  /// Performs a PUT request with an optional JSON body.
  Future<dynamic> put(
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) {
    return _send('PUT', path, body: body, queryParameters: queryParameters);
  }

  /// Performs a PATCH request with an optional JSON body.
  Future<dynamic> patch(
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) {
    return _send('PATCH', path, body: body, queryParameters: queryParameters);
  }

  /// Performs a DELETE request.
  Future<dynamic> delete(
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) {
    return _send('DELETE', path, body: body, queryParameters: queryParameters);
  }

  Future<dynamic> _send(
    String method,
    String path, {
    Object? body,
    Map<String, String>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    final request = http.Request(method, uri);

    request.headers[ApiConstants.contentTypeHeader] =
        ApiConstants.jsonContentType;

    final token = await tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      request.headers[ApiConstants.authorizationHeader] =
          '${ApiConstants.bearerPrefix} $token';
    }

    if (body != null) {
      request.body = jsonEncode(body);
    }

    final streamedResponse = await _httpClient
        .send(request)
        .timeout(ApiConstants.timeout)
        .catchError(_mapTimeout);

    final response = await http.Response.fromStream(streamedResponse);
    return _handleResponse(response);
  }

  Uri _buildUri(String path, Map<String, String>? queryParameters) {
    final uri = Uri.parse('$_baseUrl$path');
    return uri.replace(queryParameters: queryParameters);
  }

  dynamic _handleResponse(http.Response response) {
    final statusCode = response.statusCode;

    if (statusCode >= 200 && statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body);
    }

    switch (statusCode) {
      case 400:
        throw const ValidationException();
      case 401:
        throw const UnauthorizedException();
      case 403:
        throw const ForbiddenException();
      case 404:
        throw const NotFoundException();
      case 422:
        throw const ValidationException();
    }

    if (statusCode >= 500) {
      throw const ServerException();
    }

    throw const UnknownException();
  }

  Never _mapTimeout(Object error, StackTrace stackTrace) {
    throw const NetworkException('Request timed out.');
  }
}
