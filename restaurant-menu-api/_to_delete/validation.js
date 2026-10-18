'use strict';

const CATEGORIES = ['Starter', 'MainCourse', 'Dessert', 'Beverage', 'Snack'];
const SORT_FIELDS = ['dishName', 'price', 'calories'];
const ORDERS = ['asc', 'desc'];
const FIELDS = ['dishName', 'restaurant', 'category', 'price', 'calories', 'isVegetarian'];
const MAX_LIMIT = 50;

const isPlainObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v);
const has = (o, k) => Object.prototype.hasOwnProperty.call(o, k);

// Validators return { value } on success or { error } on failure.
const validators = {
  dishName(v) {
    if (typeof v !== 'string') return { error: 'dishName must be a string' };
    const s = v.trim();
    if (s.length < 3 || s.length > 60) return { error: 'dishName must be 3-60 characters' };
    return { value: s };
  },
  restaurant(v) {
    if (typeof v !== 'string') return { error: 'restaurant must be a string' };
    const s = v.trim();
    if (s.length < 2 || s.length > 40) return { error: 'restaurant must be 2-40 characters' };
    return { value: s };
  },
  category(v) {
    if (typeof v !== 'string' || !CATEGORIES.includes(v)) {
      return { error: `category must be one of ${CATEGORIES.join(', ')}` };
    }
    return { value: v };
  },
  price(v) {
    if (typeof v !== 'number' || !Number.isFinite(v)) return { error: 'price must be a number' };
    if (v <= 0 || v > 5000) return { error: 'price must be greater than 0 and at most 5000' };
    return { value: v };
  },
  calories(v) {
    if (typeof v !== 'number' || !Number.isInteger(v)) return { error: 'calories must be an integer' };
    if (v < 0 || v > 5000) return { error: 'calories must be between 0 and 5000' };
    return { value: v };
  },
  isVegetarian(v) {
    if (typeof v !== 'boolean') return { error: 'isVegetarian must be a boolean' };
    return { value: v };
  },
};

/**
 * Validate a request body.
 * partial=false (POST/PUT): every field required.
 * partial=true  (PATCH): at least one recognized field, each supplied one validated.
 * Unknown fields (including id) are dropped.
 */
function validateBody(body, partial) {
  if (!isPlainObject(body)) return { error: 'Request body must be a JSON object' };
  const data = {};
  let found = 0;
  for (const f of FIELDS) {
    if (!has(body, f)) {
      if (!partial) return { error: `${f} is required` };
      continue;
    }
    found++;
    const r = validators[f](body[f]);
    if (r.error) return { error: r.error };
    data[f] = r.value;
  }
  if (partial && found === 0) return { error: 'At least one recognized field must be provided' };
  return { data };
}

// ---- query parsing for GET /api/dishes ----
const POS_INT = /^[0-9]+$/;

function parseNumber(s) {
  if (typeof s !== 'string' || s.trim() === '') return NaN;
  const n = Number(s);
  return Number.isFinite(n) ? n : NaN;
}

function parseListQuery(q) {
  // Any repeated parameter (?page=1&page=2) arrives as an array -> invalid.
  for (const k of Object.keys(q)) {
    if (typeof q[k] !== 'string') return { error: `Invalid value for ${k}` };
  }
  const out = { page: 1, limit: 10, order: 'asc' };

  if (has(q, 'page')) {
    if (!POS_INT.test(q.page) || Number(q.page) < 1 || !Number.isSafeInteger(Number(q.page))) {
      return { error: 'page must be a positive integer' };
    }
    out.page = Number(q.page);
  }
  if (has(q, 'limit')) {
    if (!POS_INT.test(q.limit) || Number(q.limit) < 1) return { error: 'limit must be a positive integer' };
    if (Number(q.limit) > MAX_LIMIT) return { error: `limit must not exceed ${MAX_LIMIT}` };
    out.limit = Number(q.limit);
  }
  if (has(q, 'category')) {
    if (!CATEGORIES.includes(q.category)) return { error: `category must be one of ${CATEGORIES.join(', ')}` };
    out.category = q.category;
  }
  if (has(q, 'isVegetarian')) {
    if (q.isVegetarian !== 'true' && q.isVegetarian !== 'false') {
      return { error: 'isVegetarian must be "true" or "false"' };
    }
    out.isVegetarian = q.isVegetarian === 'true';
  }
  if (has(q, 'minPrice')) {
    const n = parseNumber(q.minPrice);
    if (Number.isNaN(n)) return { error: 'minPrice must be a valid number' };
    out.minPrice = n;
  }
  if (has(q, 'maxPrice')) {
    const n = parseNumber(q.maxPrice);
    if (Number.isNaN(n)) return { error: 'maxPrice must be a valid number' };
    out.maxPrice = n;
  }
  if (has(q, 'search')) {
    const s = q.search.trim().toLowerCase();
    if (s !== '') out.search = s;
  }
  if (has(q, 'sort')) {
    if (!SORT_FIELDS.includes(q.sort)) return { error: `sort must be one of ${SORT_FIELDS.join(', ')}` };
    out.sort = q.sort;
  }
  if (has(q, 'order')) {
    if (!ORDERS.includes(q.order)) return { error: 'order must be "asc" or "desc"' };
    out.order = q.order;
  }
  return { query: out };
}

module.exports = { validateBody, parseListQuery, CATEGORIES };
